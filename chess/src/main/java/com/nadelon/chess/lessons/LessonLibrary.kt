package com.nadelon.chess.lessons

import com.nadelon.chess.analysis.ThemeKey

/** Static theoretical content for each strategic theme. */
class LessonContent(
    val category: String,
    val theory: List<String>,
    val principles: List<String>,
    val drills: List<String>
)

object LessonLibrary {

    fun contentFor(theme: ThemeKey): LessonContent = when (theme) {

        ThemeKey.CENTER_NEGLECT -> LessonContent(
            category = "Strategy",
            theory = listOf(
                "Control of the centre (the d4, e4, d5 and e5 squares) is the single most important strategic idea in the opening and early middlegame. Pieces placed in or aimed at the centre influence the largest number of squares and can be redeployed to either wing quickly. A side that owns the centre dictates where the game is played.",
                "There are two healthy ways to handle the centre: occupy it with pawns (a 'classical' centre such as pawns on d4 and e4), or attack it from a distance and undermine it (the 'hypermodern' approach, e.g. fianchettoed bishops plus ...d5/...c5 breaks). What you must avoid is doing neither — making non-committal flank moves while your opponent builds an unchallenged pawn centre.",
                "When you let the centre go, your opponent gains space, your pieces have fewer good squares, and a future central pawn advance can roll over your position. Most 'I got slowly squeezed and never had a plan' games trace back to an uncontested centre."
            ),
            principles = listOf(
                "Fight for the centre from move one — either occupy it with a pawn (e4/d4) or strike at the opponent's centre with ...d5, ...c5, ...e5, or ...f5 breaks.",
                "Do not make more than one or two flank pawn moves in the opening until the centre is settled.",
                "If the opponent builds a big pawn centre, prepare a pawn break against it rather than playing around it.",
                "Remember Tarrasch: 'A central pawn majority must be mobilised; a central blockade must be broken.'"
            ),
            drills = listOf(
                "Replay 5 of your own games and pause at move 8: write down who controls each central square and why.",
                "Study model games in the Queen's Gambit and Ruy Lopez to see how masters fight for d4/e4.",
                "Practise the standard pawn breaks (...c5 vs d4, ...d5 vs e4) until they are automatic."
            )
        )

        ThemeKey.SLOW_DEVELOPMENT -> LessonContent(
            category = "Opening",
            theory = listOf(
                "The opening has three jobs: develop your pieces toward the centre, castle your king to safety, and connect your rooks. Time (tempo) is a real resource — every move you spend not developing is a move your opponent uses to get ahead.",
                "A classic mistake is to move the same piece twice, grab pawns with undeveloped pieces, or push pawns instead of bringing out knights and bishops. The punishment is often a quick attack: the side that is fully developed simply has more force pointed at the action.",
                "Develop with purpose — knights before bishops as a guideline, toward squares where they hit the centre, and get castled before opening the position."
            ),
            principles = listOf(
                "Aim to have both knights and both bishops developed and to be castled by roughly move 10.",
                "Knights before bishops; develop toward the centre, not the rim ('a knight on the rim is dim').",
                "Don't move the same piece twice in the opening without a concrete reason.",
                "Don't start an attack or grab material before your pieces are out and your king is safe."
            ),
            drills = listOf(
                "Set a personal rule for 10 games: do not capture material before move 10 unless it wins a piece or is clearly forced.",
                "After each opening, count how many moves it took you to castle — try to lower that number.",
                "Solve 'develop and castle' puzzles where the task is the best developing move, not a tactic."
            )
        )

        ThemeKey.EARLY_QUEEN -> LessonContent(
            category = "Opening",
            theory = listOf(
                "The queen is your most valuable piece, so it is also the easiest to harass. Bringing it out early lets your opponent develop their minor pieces with tempo — each time they attack your queen, they gain a free developing move while you scramble to safety.",
                "There are exceptions (some openings use early queen moves with concrete justification), but as a rule the queen should come out after the minor pieces, to a square where it cannot be kicked around."
            ),
            principles = listOf(
                "Develop knights and bishops first; bring the queen out only when it has a safe, useful square.",
                "Before any early queen move, ask: 'Can this be attacked, and will I lose time moving it again?'",
                "Use the queen to support central breaks and piece activity, not to chase early pawns."
            ),
            drills = listOf(
                "Review games where your queen was chased — note how many tempi you lost.",
                "Study lines of your openings to learn the correct, safe square for the queen."
            )
        )

        ThemeKey.KING_IN_CENTER -> LessonContent(
            category = "King safety",
            theory = listOf(
                "A king in the centre is a king in danger. The central files (d and e) tend to open as pawns are traded, and an uncastled king sitting on that battlefield becomes the target of every tactic — checks, pins, and sacrifices that rip the position open.",
                "Castling does two jobs at once: it tucks the king behind a pawn shield and activates a rook. Delaying it 'just one more move' is one of the most common causes of sudden disasters below master level.",
                "If you cannot castle for some reason, you must keep the centre closed and be extremely careful about opening lines."
            ),
            principles = listOf(
                "Castle early — usually within the first 10 moves — unless there is a concrete reason not to.",
                "Do not open the centre while your king is still there.",
                "If your king is stuck in the centre, prioritise getting it to safety over winning material.",
                "Watch for opponent piece sacrifices that open the e- or d-file against your king."
            ),
            drills = listOf(
                "In your next 10 games, make castling a priority and note any game where delaying it hurt you.",
                "Study famous 'king hunt' games (e.g. Morphy's Opera Game) to feel the danger of a central king."
            )
        )

        ThemeKey.WEAK_DARK_SQUARES, ThemeKey.WEAK_LIGHT_SQUARES -> LessonContent(
            category = "Pawn structure",
            theory = listOf(
                "Squares come in two colours, and the pieces that guard them matter. Pawns only ever guard squares of one colour from a given position, and once a bishop of a particular colour is gone, the squares of that colour can become permanently weak — there is simply nothing left to control them.",
                "A 'colour complex' weakness is created in two main ways: by trading or losing the bishop of that colour, or by advancing several pawns onto the opposite colour (for example ...g6, ...h6, ...f6 weakens the dark squares around a king). The opponent then plants knights and the remaining bishop on those holes, and they cannot be driven away.",
                "Around the king this is especially deadly: a dark-square or light-square attack with no defending bishop of that colour is a classic way games are lost. The famous 'dark-squared bind' arises exactly this way.",
                "Think of each move as also a commitment about square colour: 'If I play this pawn move or trade this bishop, which colour squares am I giving up forever?'"
            ),
            principles = listOf(
                "Be reluctant to trade the bishop that defends the colour of squares around your own king.",
                "Avoid pushing several pawns onto the same colour — it concedes the other colour permanently.",
                "If you are weak on one colour, trade off the opponent's piece that exploits it (especially their same-coloured bishop or a dominating knight).",
                "When attacking, target the colour your opponent cannot defend, and bring a knight or bishop to a hole there."
            ),
            drills = listOf(
                "In your games, find positions where you traded a bishop and ask which squares it left undefended.",
                "Study games featuring colour-complex domination (e.g. Botvinnik and Karpov dark-square binds).",
                "Practise spotting permanent holes: squares your opponent can never attack with a pawn."
            )
        )

        ThemeKey.PASSIVE_ROOKS -> LessonContent(
            category = "Piece activity",
            theory = listOf(
                "Rooks are long-range pieces that come alive on open and semi-open files — files with no pawns, or no pawns of your own. A rook stuck behind its own pawns does almost nothing; the same rook on an open file can dominate, invade the seventh rank, and decide the game.",
                "Identifying and seizing the right file is a core middlegame skill. Often both sides race to occupy the only open file; whoever gets there first (and can hold it, sometimes by doubling rooks) gains a lasting edge.",
                "Rooks also belong behind passed pawns — yours to push them, the opponent's to stop them."
            ),
            principles = listOf(
                "Put rooks on open files; if none exist, on semi-open files or files likely to open.",
                "Double rooks to fight for and control a contested file.",
                "Aim a rook at the seventh (or second) rank, where it attacks pawns and traps the king.",
                "Place rooks behind passed pawns, not in front of them."
            ),
            drills = listOf(
                "In each middlegame you reach, list the open/semi-open files and ask if your rooks use them.",
                "Practise the 'rook on the 7th' technique in endgame studies.",
                "Replay games where your rooks never moved — find when you could have activated them."
            )
        )

        ThemeKey.NO_COUNTERPLAY -> LessonContent(
            category = "Strategy",
            theory = listOf(
                "Space is the territory your pawns and pieces control. A space advantage lets the bigger side manoeuvre freely while the cramped side trips over its own pieces. If you simply sit and defend against more space, you will slowly be squeezed.",
                "The antidote to a space disadvantage is counterplay — usually a pawn break that opens lines and frees your pieces, or active piece play in a specific area. A guiding rule from Steinitz and refined by countless masters: 'Meet a flank attack with a counterstrike in the centre.' If your opponent commits pawns to attack on a wing, the centre is the place to hit back, because central lines are the shortest route to their position and an opened centre exposes the over-extended wing.",
                "Equally important: 'the cramped side should seek exchanges.' Trading a pair or two of pieces relieves the congestion and makes your remaining pieces useful.",
                "The mistake is passivity — shuffling pieces with no plan while the opponent improves. Always be asking: where is my break, and which pieces do I want to trade?"
            ),
            principles = listOf(
                "If the opponent attacks on a wing, look first for a central pawn break or central piece play.",
                "When cramped, look to trade pieces to relieve the lack of space.",
                "Identify your pawn break early (…c5, …d5, …f5, …b5 depending on structure) and prepare it.",
                "Don't just react — give yourself an active plan even a tempo before you're forced to."
            ),
            drills = listOf(
                "In cramped positions from your games, find the pawn break you missed.",
                "Study King's Indian and Benoni games to learn dynamic counterplay against space.",
                "Practise the rule 'flank attack → central counter' on tactics-trainer middlegames."
            )
        )

        ThemeKey.PAWN_WEAKNESSES -> LessonContent(
            category = "Pawn structure",
            theory = listOf(
                "Pawns are the skeleton of the position: they cannot move backward, so the weaknesses they create are permanent. Isolated pawns (no friendly pawns on adjacent files), doubled pawns (two on one file), and backward pawns (stuck behind their neighbours on a half-open file) become long-term targets the opponent can pile up on.",
                "Not every structural concession is bad — an isolated queen's pawn, for instance, grants attacking chances in return for the weakness. The key is to create such weaknesses consciously, for compensation, rather than drifting into them.",
                "When you do have a weakness, the defensive technique is to keep pieces active and seek exchanges that reduce the pressure; when your opponent has one, fix it in place and attack it with everything."
            ),
            principles = listOf(
                "Before a capture or pawn push, ask what permanent structural change it makes.",
                "Avoid creating isolated, doubled or backward pawns unless you get clear activity in return.",
                "Fix and blockade enemy weaknesses (especially isolated pawns) before attacking them.",
                "Trade pieces when defending a weakness; keep pieces on when attacking one."
            ),
            drills = listOf(
                "Catalogue the pawn weaknesses you created across your games and what caused each.",
                "Study isolated-queen's-pawn (IQP) middlegames from both sides.",
                "Practise endgames with one structural weakness to feel how they are won and held."
            )
        )

        ThemeKey.CONVERSION_FAIL -> LessonContent(
            category = "Technique",
            theory = listOf(
                "Winning a winning position is a separate skill from getting one. Once you are clearly ahead (a couple of pawns or a piece), the priority shifts from grabbing more to converting cleanly: trade pieces (not pawns), keep the king safe, and avoid giving any counterplay.",
                "The classic rule is 'when ahead in material, exchange pieces; when behind, exchange pawns.' Each trade of pieces brings you closer to a trivially winning endgame and removes the opponent's attacking potential. Greed and complacency — going for a flashy finish or relaxing — are what turn wins into draws and losses.",
                "Practical conversion also means choosing the simplest path, not the objectively best-by-one-centipawn path. Reduce risk, simplify, and steer toward endgames you know how to win."
            ),
            principles = listOf(
                "When materially ahead, trade pieces and steer toward a simple winning endgame.",
                "Keep your king safe and deny counterplay even at the cost of a little material.",
                "Don't relax after winning material — winning positions require accurate technique.",
                "Prefer the clearest winning method over the flashiest one."
            ),
            drills = listOf(
                "Study basic winning endgames (K+Q vs K, K+R vs K, two pawns up) until they are automatic.",
                "Replay your blown winning games and mark the exact move the conversion went wrong.",
                "Practise 'winning won positions' versus the engine from a +3 advantage."
            )
        )

        ThemeKey.LATE_GAME_COLLAPSE -> LessonContent(
            category = "Endgame",
            theory = listOf(
                "Many players are strong in the opening and middlegame but lose the thread late — in the endgame or in time trouble. Endgames reward precise knowledge (key theoretical positions), king activity, and calm calculation rather than memorised opening moves.",
                "Two things drive late collapses: thin endgame knowledge (not knowing the standard techniques, so you burn time and go wrong), and time/energy management (rushing the last moves). The fix is partly study and partly habit — keep checking for opponent resources right to the end.",
                "In the endgame the king becomes a fighting piece, passed pawns gain enormous value, and a single tempo often decides. Slow down exactly when the position simplifies — that is when accuracy matters most."
            ),
            principles = listOf(
                "Activate your king in the endgame — it is a strong piece once queens are off.",
                "Learn the essential theoretical endings (opposition, king-and-pawn, Lucena/Philidor in rook endings).",
                "Manage your clock so you have time for the endgame, not just the opening.",
                "Keep checking for opponent counterplay and stalemate tricks until the game is truly over."
            ),
            drills = listOf(
                "Drill king-and-pawn and basic rook endgames daily for two weeks.",
                "Review your late-game losses and identify whether it was knowledge or time.",
                "Play training games where you reach an endgame and must convert with limited time."
            )
        )

        ThemeKey.TACTICAL_BLUNDERS -> LessonContent(
            category = "Tactics",
            theory = listOf(
                "Tactics decide most games below master level. A single hanging piece or missed fork can undo a beautifully played position. Tactical strength comes from two habits: pattern recognition (instantly seeing forks, pins, skewers, discovered attacks) and a checking routine before every move.",
                "The most reliable anti-blunder habit is the blunder-check: before you play, look at every check, capture, and threat your opponent will have in reply. Most blunders are not deep — they are one-move oversights that a quick safety check would catch.",
                "Calculation discipline matters too: identify candidate moves, calculate forcing lines to a quiet position, and verify the final position is actually good for you before committing."
            ),
            principles = listOf(
                "Before every move, check all opponent checks, captures and threats (CCT).",
                "Make sure your pieces are defended — count attackers and defenders on each.",
                "Look for your own forcing moves first: checks, captures, threats.",
                "Calculate forcing lines to the end before trusting an evaluation."
            ),
            drills = listOf(
                "Solve 15–30 tactics puzzles a day, focused on the patterns you miss most.",
                "Adopt a strict pre-move blunder-check for 20 games and count how many blunders it prevents.",
                "Review each tactical loss and re-solve the position as a puzzle."
            )
        )
    }
}
