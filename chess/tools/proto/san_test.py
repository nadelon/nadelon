from chess import Board

# Morphy "Opera Game", 1858
moves = "e4 e5 Nf3 d6 d4 Bg4 dxe5 Bxf3 Qxf3 dxe5 Bc4 Nf6 Qb3 Qe7 Nc3 c6 Bg5 b5 Nxb5 cxb5 Bxb5+ Nbd7 O-O-O Rd8 Rxd7 Rxd7 Rd1 Qe6 Bxd7+ Nxd7 Qb8+ Nxb8 Rd8#".split()

b = Board.from_fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
ok = True
for i, tok in enumerate(moves):
    legal = b.legal_moves()
    match = None
    for mv in legal:
        if b.san(mv, legal) == tok:
            match = mv; break
    if match is None:
        print(f"FAIL move {i+1}: '{tok}' not found. Legal SANs:", [b.san(m, legal) for m in legal][:12])
        ok = False
        break
    b.make(match)
print("FINAL FEN:", b.to_fen())
# After 17.Rd8# black is checkmated, white just moved -> black to move, in check, no moves
print("Black in check:", b.in_check(1), "Black legal moves:", len(b.legal_moves()))
print("SAN ROUNDTRIP:", "PASS" if ok else "FAIL")
