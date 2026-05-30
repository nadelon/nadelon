"""Port of Evaluation + Search + MoveQuality sign logic, to validate conventions."""
import math
from chess import Board, WHITE, BLACK, file_of, rank_of, sq_of

VALUE = {'P':100,'N':320,'B':330,'R':500,'Q':900,'K':0}
MATE = 30000
INF = 1_000_000

def evaluate(b):  # White POV
    score = 0
    for i in range(64):
        p = b.sq[i]
        if p == '.': continue
        v = VALUE[p.upper()]
        score += v if p.isupper() else -v
    return score

def win_prob(cp):
    c = max(-1500, min(1500, cp))
    return 1.0/(1.0+math.exp(-0.00368208*c))

def order(b, moves):
    def key(mv):
        victim = b.sq[mv[1]]
        if victim != '.':
            return 1000 + VALUE[victim.upper()]*10 - VALUE[b.sq[mv[0]].upper()]
        return 0
    return sorted(moves, key=key, reverse=True)

def quiesce(b, alpha, beta):
    me = b.turn
    sign = 1 if me == WHITE else -1
    stand = evaluate(b)*sign
    if stand >= beta: return beta
    if stand > alpha: alpha = stand
    caps = order(b, [m for m in b.legal_moves() if b.sq[m[1]] != '.' or m[3]=='ep' or m[2]])
    for mv in caps:
        snap = b.make(mv)
        score = -quiesce(b, -beta, -alpha)
        b.unmake(snap)
        if score >= beta: return beta
        if score > alpha: alpha = score
    return alpha

def negamax(b, depth, alpha, beta):
    me = b.turn
    if depth <= 0: return quiesce(b, alpha, beta)
    legal = order(b, b.legal_moves())
    if not legal:
        return -MATE - depth if b.in_check(me) else 0
    for mv in legal:
        snap = b.make(mv)
        score = -negamax(b, depth-1, -beta, -alpha)
        b.unmake(snap)
        if score >= beta: return beta
        if score > alpha: alpha = score
    return alpha

def search(b, depth):
    me = b.turn
    sign = 1 if me == WHITE else -1
    alpha = -INF; beta = INF; best = None
    legal = order(b, b.legal_moves())
    if not legal:
        return ((-MATE*sign) if b.in_check(me) else 0), None
    for mv in legal:
        snap = b.make(mv)
        score = -negamax(b, depth-1, -beta, -alpha)
        b.unmake(snap)
        if score > alpha:
            alpha = score; best = mv
    return alpha*sign, best  # White POV

def classify(b, played, depth):
    me = b.turn
    sign = 1 if me == WHITE else -1
    before_score, best = search(b, depth)
    cp_before = before_score*sign
    snap = b.make(played)
    after_score, _ = search(b, depth)
    cp_after = after_score*sign
    b.unmake(snap)
    loss = max(0.0, win_prob(cp_before) - win_prob(cp_after))
    return cp_before, cp_after, loss, best

if __name__ == '__main__':
    out = []
    # 1. startpos eval ~ 0
    b = Board.from_fen(Board.from_fen.__self__.STARTPOS if False else "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    out.append(f"startpos eval = {evaluate(b)} (expect 0)")

    # 2. White to move can capture a free queen on d5 with pawn? Use simple: white Qxd8 winning queen.
    # Position: white queen can take black queen for free.
    b = Board.from_fen("3qk3/8/8/8/8/8/8/3QK3 w - - 0 1")
    sc, best = search(b, 3)
    out.append(f"Q vs Q symmetric: score(White POV)={sc} best={best and (file_of(best[0]),rank_of(best[0]),file_of(best[1]),rank_of(best[1]))}")

    # 3. White is up a queen -> score should be strongly positive for white
    b = Board.from_fen("4k3/8/8/8/8/8/8/3QK3 w - - 0 1")
    sc, best = search(b, 3)
    out.append(f"White +Q: score(White POV)={sc} (expect strongly +, ~900)")

    # 4. Black to move, up a queen -> White POV should be strongly negative
    b = Board.from_fen("3qk3/8/8/8/8/8/8/4K3 b - - 0 1")
    sc, best = search(b, 3)
    out.append(f"Black +Q to move: score(White POV)={sc} (expect strongly -, ~-900)")

    # 5. Blunder detection: White to move has free capture Bxf7+ winning, but plays a quiet move hanging nothing vs hanging queen.
    # Make white hang the queen: white Q on d1 can be taken by black bishop on a4? set up.
    # Position: white queen d4, black rook d8 on open d-file; white to move plays Qd5?? allowing ...Rxd5. vs safe Qa1.
    b = Board.from_fen("3rk3/8/8/8/3Q4/8/8/4K3 w - - 0 1")
    # played: Qd5 (d4->d5) which is still defended? black rook d8 can take d5 (Rxd5) winning queen, queen undefended.
    played = (sq_of(3,3), sq_of(3,4), '', '')  # d4-d5
    cb, ca, loss, best = classify(b, played, 3)
    bsan = best and "best="+str((file_of(best[0]),rank_of(best[0]),file_of(best[1]),rank_of(best[1])))
    out.append(f"Hang-queen move Qd5: cp_before={cb} cp_after={ca} winloss={loss:.2f} (expect big drop) {bsan}")

    print("\n".join(out))
