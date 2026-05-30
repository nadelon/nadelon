"""
Reference chess move generator + SAN + perft.
Used to lock the algorithm before porting to Kotlin (com.nadelon.chess.core).
Board is a flat array of 64 squares, index = rank*8 + file, a1=0, h8=63.
Pieces: uppercase = white, lowercase = black, '.' = empty.
"""

WHITE, BLACK = 0, 1

KNIGHT_OFFS = [(1, 2), (2, 1), (2, -1), (1, -2), (-1, -2), (-2, -1), (-2, 1), (-1, 2)]
KING_OFFS = [(1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1), (1, -1)]
BISHOP_DIRS = [(1, 1), (1, -1), (-1, 1), (-1, -1)]
ROOK_DIRS = [(1, 0), (-1, 0), (0, 1), (0, -1)]
QUEEN_DIRS = BISHOP_DIRS + ROOK_DIRS


def file_of(sq): return sq & 7
def rank_of(sq): return sq >> 3
def sq_of(f, r): return r * 8 + f
def on_board(f, r): return 0 <= f < 8 and 0 <= r < 8


class Board:
    def __init__(self):
        self.sq = ['.'] * 64
        self.turn = WHITE
        self.castle = set()  # 'K','Q','k','q'
        self.ep = -1         # en passant target square or -1
        self.half = 0
        self.full = 1

    @staticmethod
    def from_fen(fen):
        b = Board()
        parts = fen.split()
        rows = parts[0].split('/')
        for r in range(8):
            row = rows[7 - r]
            f = 0
            for ch in row:
                if ch.isdigit():
                    f += int(ch)
                else:
                    b.sq[sq_of(f, r)] = ch
                    f += 1
        b.turn = WHITE if parts[1] == 'w' else BLACK
        b.castle = set(c for c in parts[2] if c in 'KQkq')
        b.ep = -1 if parts[3] == '-' else sq_of(ord(parts[3][0]) - 97, int(parts[3][1]) - 1)
        b.half = int(parts[4]) if len(parts) > 4 else 0
        b.full = int(parts[5]) if len(parts) > 5 else 1
        return b

    def to_fen(self):
        rows = []
        for r in range(7, -1, -1):
            row = ''
            empty = 0
            for f in range(8):
                p = self.sq[sq_of(f, r)]
                if p == '.':
                    empty += 1
                else:
                    if empty:
                        row += str(empty); empty = 0
                    row += p
            if empty:
                row += str(empty)
            rows.append(row)
        castle = ''.join(c for c in 'KQkq' if c in self.castle) or '-'
        ep = '-' if self.ep < 0 else chr(file_of(self.ep) + 97) + str(rank_of(self.ep) + 1)
        return f"{'/'.join(rows)} {'w' if self.turn == WHITE else 'b'} {castle} {ep} {self.half} {self.full}"

    def color_of(self, p):
        if p == '.': return -1
        return WHITE if p.isupper() else BLACK

    def find_king(self, color):
        k = 'K' if color == WHITE else 'k'
        for i in range(64):
            if self.sq[i] == k:
                return i
        return -1

    def attacked_by(self, sq, by_color):
        """Is `sq` attacked by side `by_color`?"""
        f0, r0 = file_of(sq), rank_of(sq)
        # pawns
        if by_color == WHITE:
            for df in (-1, 1):
                f, r = f0 + df, r0 - 1
                if on_board(f, r) and self.sq[sq_of(f, r)] == 'P':
                    return True
        else:
            for df in (-1, 1):
                f, r = f0 + df, r0 + 1
                if on_board(f, r) and self.sq[sq_of(f, r)] == 'p':
                    return True
        # knights
        kn = 'N' if by_color == WHITE else 'n'
        for df, dr in KNIGHT_OFFS:
            f, r = f0 + df, r0 + dr
            if on_board(f, r) and self.sq[sq_of(f, r)] == kn:
                return True
        # king
        kg = 'K' if by_color == WHITE else 'k'
        for df, dr in KING_OFFS:
            f, r = f0 + df, r0 + dr
            if on_board(f, r) and self.sq[sq_of(f, r)] == kg:
                return True
        # sliders: bishop/queen diagonal
        bq = ('B', 'Q') if by_color == WHITE else ('b', 'q')
        for df, dr in BISHOP_DIRS:
            f, r = f0 + df, r0 + dr
            while on_board(f, r):
                p = self.sq[sq_of(f, r)]
                if p != '.':
                    if p in bq: return True
                    break
                f += df; r += dr
        # rook/queen straight
        rq = ('R', 'Q') if by_color == WHITE else ('r', 'q')
        for df, dr in ROOK_DIRS:
            f, r = f0 + df, r0 + dr
            while on_board(f, r):
                p = self.sq[sq_of(f, r)]
                if p != '.':
                    if p in rq: return True
                    break
                f += df; r += dr
        return False

    def in_check(self, color):
        return self.attacked_by(self.find_king(color), 1 - color)

    def pseudo_moves(self):
        """Generate pseudo-legal moves as tuples (frm, to, promo, flag)
        flag: '', 'ep', '2', 'O-O', 'O-O-O'"""
        moves = []
        me = self.turn
        for frm in range(64):
            p = self.sq[frm]
            if p == '.' or self.color_of(p) != me:
                continue
            f0, r0 = file_of(frm), rank_of(frm)
            pt = p.upper()
            if pt == 'P':
                fwd = 1 if me == WHITE else -1
                start_rank = 1 if me == WHITE else 6
                promo_rank = 7 if me == WHITE else 0
                # single push
                r1 = r0 + fwd
                if on_board(f0, r1) and self.sq[sq_of(f0, r1)] == '.':
                    to = sq_of(f0, r1)
                    if r1 == promo_rank:
                        for pr in 'QRBN':
                            moves.append((frm, to, pr, ''))
                    else:
                        moves.append((frm, to, '', ''))
                        # double push
                        if r0 == start_rank:
                            r2 = r0 + 2 * fwd
                            if self.sq[sq_of(f0, r2)] == '.':
                                moves.append((frm, sq_of(f0, r2), '', '2'))
                # captures
                for df in (-1, 1):
                    f1 = f0 + df
                    if not on_board(f1, r1):
                        continue
                    to = sq_of(f1, r1)
                    tp = self.sq[to]
                    if tp != '.' and self.color_of(tp) != me:
                        if r1 == promo_rank:
                            for pr in 'QRBN':
                                moves.append((frm, to, pr, ''))
                        else:
                            moves.append((frm, to, '', ''))
                    elif to == self.ep:
                        moves.append((frm, to, '', 'ep'))
            elif pt == 'N':
                for df, dr in KNIGHT_OFFS:
                    f, r = f0 + df, r0 + dr
                    if on_board(f, r):
                        tp = self.sq[sq_of(f, r)]
                        if tp == '.' or self.color_of(tp) != me:
                            moves.append((frm, sq_of(f, r), '', ''))
            elif pt == 'K':
                for df, dr in KING_OFFS:
                    f, r = f0 + df, r0 + dr
                    if on_board(f, r):
                        tp = self.sq[sq_of(f, r)]
                        if tp == '.' or self.color_of(tp) != me:
                            moves.append((frm, sq_of(f, r), '', ''))
                # castling
                if me == WHITE and frm == sq_of(4, 0):
                    if 'K' in self.castle and self.sq[sq_of(5, 0)] == '.' and self.sq[sq_of(6, 0)] == '.' \
                            and self.sq[sq_of(7, 0)] == 'R' \
                            and not self.attacked_by(sq_of(4, 0), BLACK) \
                            and not self.attacked_by(sq_of(5, 0), BLACK) \
                            and not self.attacked_by(sq_of(6, 0), BLACK):
                        moves.append((frm, sq_of(6, 0), '', 'O-O'))
                    if 'Q' in self.castle and self.sq[sq_of(3, 0)] == '.' and self.sq[sq_of(2, 0)] == '.' \
                            and self.sq[sq_of(1, 0)] == '.' and self.sq[sq_of(0, 0)] == 'R' \
                            and not self.attacked_by(sq_of(4, 0), BLACK) \
                            and not self.attacked_by(sq_of(3, 0), BLACK) \
                            and not self.attacked_by(sq_of(2, 0), BLACK):
                        moves.append((frm, sq_of(2, 0), '', 'O-O-O'))
                if me == BLACK and frm == sq_of(4, 7):
                    if 'k' in self.castle and self.sq[sq_of(5, 7)] == '.' and self.sq[sq_of(6, 7)] == '.' \
                            and self.sq[sq_of(7, 7)] == 'r' \
                            and not self.attacked_by(sq_of(4, 7), WHITE) \
                            and not self.attacked_by(sq_of(5, 7), WHITE) \
                            and not self.attacked_by(sq_of(6, 7), WHITE):
                        moves.append((frm, sq_of(6, 7), '', 'O-O'))
                    if 'q' in self.castle and self.sq[sq_of(3, 7)] == '.' and self.sq[sq_of(2, 7)] == '.' \
                            and self.sq[sq_of(1, 7)] == '.' and self.sq[sq_of(0, 7)] == 'r' \
                            and not self.attacked_by(sq_of(4, 7), WHITE) \
                            and not self.attacked_by(sq_of(3, 7), WHITE) \
                            and not self.attacked_by(sq_of(2, 7), WHITE):
                        moves.append((frm, sq_of(2, 7), '', 'O-O-O'))
            else:
                dirs = BISHOP_DIRS if pt == 'B' else ROOK_DIRS if pt == 'R' else QUEEN_DIRS
                for df, dr in dirs:
                    f, r = f0 + df, r0 + dr
                    while on_board(f, r):
                        to = sq_of(f, r)
                        tp = self.sq[to]
                        if tp == '.':
                            moves.append((frm, to, '', ''))
                        else:
                            if self.color_of(tp) != me:
                                moves.append((frm, to, '', ''))
                            break
                        f += df; r += dr
        return moves

    def make(self, mv):
        """Apply move, return undo info."""
        frm, to, promo, flag = mv
        undo = (self.sq[frm], self.sq[to], self.castle.copy(), self.ep, self.half, self.full, self.turn)
        p = self.sq[frm]
        me = self.turn
        self.ep = -1
        self.half += 1
        if p.upper() == 'P' or self.sq[to] != '.':
            self.half = 0
        # move piece
        self.sq[to] = p
        self.sq[frm] = '.'
        if flag == '2':
            self.ep = (frm + to) // 2
        elif flag == 'ep':
            cap_sq = sq_of(file_of(to), rank_of(frm))
            self.sq[cap_sq] = '.'
        elif flag == 'O-O':
            if me == WHITE:
                self.sq[sq_of(5, 0)] = 'R'; self.sq[sq_of(7, 0)] = '.'
            else:
                self.sq[sq_of(5, 7)] = 'r'; self.sq[sq_of(7, 7)] = '.'
        elif flag == 'O-O-O':
            if me == WHITE:
                self.sq[sq_of(3, 0)] = 'R'; self.sq[sq_of(0, 0)] = '.'
            else:
                self.sq[sq_of(3, 7)] = 'r'; self.sq[sq_of(0, 7)] = '.'
        if promo:
            self.sq[to] = promo if me == WHITE else promo.lower()
        # update castling rights
        for s, c in ((sq_of(4, 0), ('K', 'Q')), (sq_of(0, 0), ('Q',)), (sq_of(7, 0), ('K',)),
                     (sq_of(4, 7), ('k', 'q')), (sq_of(0, 7), ('q',)), (sq_of(7, 7), ('k',))):
            if frm == s or to == s:
                for cc in c:
                    self.castle.discard(cc)
        if me == BLACK:
            self.full += 1
        self.turn = 1 - me
        return (frm, to, promo, flag, undo)

    def unmake(self, snap):
        frm, to, promo, flag, undo = snap
        sfrm, sto, castle, ep, half, full, turn = undo
        me = turn
        self.sq[frm] = sfrm
        self.sq[to] = sto
        if flag == 'ep':
            cap_sq = sq_of(file_of(to), rank_of(frm))
            self.sq[cap_sq] = 'p' if me == WHITE else 'P'
        elif flag == 'O-O':
            if me == WHITE:
                self.sq[sq_of(7, 0)] = 'R'; self.sq[sq_of(5, 0)] = '.'
            else:
                self.sq[sq_of(7, 7)] = 'r'; self.sq[sq_of(5, 7)] = '.'
        elif flag == 'O-O-O':
            if me == WHITE:
                self.sq[sq_of(0, 0)] = 'R'; self.sq[sq_of(3, 0)] = '.'
            else:
                self.sq[sq_of(0, 7)] = 'r'; self.sq[sq_of(3, 7)] = '.'
        self.castle = castle
        self.ep = ep
        self.half = half
        self.full = full
        self.turn = turn

    def legal_moves(self):
        out = []
        me = self.turn
        for mv in self.pseudo_moves():
            snap = self.make(mv)
            if not self.attacked_by(self.find_king(me), 1 - me):
                out.append(mv)
            self.unmake(snap)
        return out

    def san(self, mv, legal):
        frm, to, promo, flag = mv
        if flag == 'O-O':
            base = 'O-O'
        elif flag == 'O-O-O':
            base = 'O-O-O'
        else:
            p = self.sq[frm]
            pt = p.upper()
            capture = self.sq[to] != '.' or flag == 'ep'
            dest = chr(file_of(to) + 97) + str(rank_of(to) + 1)
            if pt == 'P':
                if capture:
                    base = chr(file_of(frm) + 97) + 'x' + dest
                else:
                    base = dest
                if promo:
                    base += '=' + promo
            else:
                # disambiguation
                same = [m for m in legal if m[1] == to and self.sq[m[0]] == p and m[0] != frm]
                disamb = ''
                if same:
                    same_file = any(file_of(m[0]) == file_of(frm) for m in same)
                    same_rank = any(rank_of(m[0]) == rank_of(frm) for m in same)
                    if not same_file:
                        disamb = chr(file_of(frm) + 97)
                    elif not same_rank:
                        disamb = str(rank_of(frm) + 1)
                    else:
                        disamb = chr(file_of(frm) + 97) + str(rank_of(frm) + 1)
                base = pt + disamb + ('x' if capture else '') + dest
        # check / mate suffix
        snap = self.make(mv)
        opp = self.turn
        chk = self.attacked_by(self.find_king(opp), 1 - opp)
        has_moves = len(self.legal_moves()) > 0
        self.unmake(snap)
        if chk:
            base += '#' if not has_moves else '+'
        return base


def perft(board, depth):
    if depth == 0:
        return 1
    total = 0
    for mv in board.legal_moves():
        snap = board.make(mv)
        total += perft(board, depth - 1)
        board.unmake(snap)
    return total


if __name__ == '__main__':
    START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    KIWIPETE = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
    POS3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
    POS4 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
    POS5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"

    tests = [
        (START, [1, 20, 400, 8902, 197281]),
        (KIWIPETE, [1, 48, 2039, 97862]),
        (POS3, [1, 14, 191, 2812, 43238]),
        (POS4, [1, 6, 264, 9467]),
        (POS5, [1, 44, 1486, 62379]),
    ]
    ok = True
    for fen, expected in tests:
        for d, exp in enumerate(expected):
            b = Board.from_fen(fen)
            got = perft(b, d)
            status = 'OK' if got == exp else 'FAIL'
            if got != exp:
                ok = False
            print(f"perft({d}) {status}: got {got} exp {exp}  [{fen[:30]}...]")
    print("ALL PASS" if ok else "SOME FAILED")
