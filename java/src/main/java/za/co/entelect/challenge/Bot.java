package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;
import za.co.entelect.challenge.enums.PowerUpType;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {
        Worm specialEnemyWorm = getFirstWormInSpecialRange();
        if (specialEnemyWorm != null)
        {
            if (currentWorm.id == 3) {
                if (currentWorm.snowballs.count > 0 && specialEnemyWorm.roundsUntilUnfrozen < 1) {
                    return new SnowballCommand(specialEnemyWorm.position.x, specialEnemyWorm.position.y);
                }
            }
            else if (currentWorm.id == 2) {
                if (currentWorm.bananaBombs.count > 0) {
                    return new BananaCommand(specialEnemyWorm.position.x, specialEnemyWorm.position.y);
                }
            }
        }

        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        Worm enemyAgent2 = getOpponentByProfession(2);
        if (enemyAgent2 != null){   
            Cell block =  positionNearestToXY(currentWorm.position.x, currentWorm.position.y, enemyAgent2.position.x, enemyAgent2.position.y);
            return MoveOrDig(block);
        }
        Worm enemyAgent3 = getOpponentByProfession(3);
        if (enemyAgent3 != null){
            Cell block =  positionNearestToXY(currentWorm.position.x, currentWorm.position.y, enemyAgent3.position.x, enemyAgent3.position.y);
            return MoveOrDig(block);
        }
        Worm enemyAgent1 = getOpponentByProfession(1);
        if (enemyAgent1 != null){
            Cell block =  positionNearestToXY(currentWorm.position.x, currentWorm.position.y, enemyAgent1.position.x, enemyAgent1.position.y);
            return MoveOrDig(block);
        }

        // List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        // int cellIdx = random.nextInt(surroundingBlocks.size());

        // Cell block = surroundingBlocks.get(cellIdx);
        Cell block = positionFarthestFromNearestEnemy(currentWorm.position.x, currentWorm.position.y);
        // if (block.type == CellType.AIR) {
        //     return new MoveCommand(block.x, block.y);
        // } else if (block.type == CellType.DIRT) {
        //     return new DigCommand(block.x, block.y);
        // }

        // return new DoNothingCommand();
        return MoveOrDig(block);
    }

    private Worm getFirstWormInRange() {
        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0) {
                return enemyWorm;
            }
        }

        return null;
    }

    private Worm getFirstWormInSpecialRange() {
        for (int i = 0;i < 3;i++)
        {
            if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, opponent.worms[i].position.x, opponent.worms[i].position.y) <= 5 && opponent.worms[i].health > 0)
            {
                return opponent.worms[i];
            }
        }
        return null;
    } // kita bisa pake constructFireDirectionLines
    // constructFireDirectionLines(5) itu harusnya udah ngegambar rangenya
    // bedanya di bagian if (cell.type != CellType.AIR)
    // harusnya kalo dirt juga bisa buat special
    // 
    // Eh ler cuman kalo si banana sama snowball itu dia kagak perlu line tau ler beda sama shoot
    // Jadinya gabisa pake construct FireDirectionLines ga sih

    // Gua kira dia line nya sama kayak weapon shoot bedanya range nya doang 5
    // Kalo di commandnya pake koordinat tapi ga pake W, NW, dll

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private List<Cell> getSurroundingCellsNoLava(int x, int y) {
        // Mengambil Cells yang bersebelahan yang bukan cell bertipe lava
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (i != x && j != y && isValidCoordinate(i, j) && gameState.map[j][i].type != CellType.LAVA && gameState.map[j][i].type != CellType.DEEP_SPACE) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }
        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    private Worm getNearestEnemyWorm(int x, int y) {
        // input int x dan int y merupakan posisi currentWorm saat ini
        // Mencari worm musuh yang paling dekat dengan currentWorm
        int[] distanceList = new int[3];
        for (int i = 0; i < 3; i++) {
            Worm enemyWorm = opponent.worms[i];
            if (enemyWorm.health > 0) {
                distanceList[i] = euclideanDistance(x, y, enemyWorm.position.x, enemyWorm.position.y);
            }
            else {
                distanceList[i] = -999; // Penanda enemy sudah mati
            }
        }
        int returnId = -1; // check lagi ntar
        int minDistance = 100;
        for (int i = 0; i < 3; i++) {
            if (distanceList[i] != -999) {
                if (distanceList[i] < minDistance) {
                    returnId = i;
                    minDistance = distanceList[i];
                }
            }
            else {
                continue;
            }
        }
        return (opponent.worms[returnId]);
    }

    private Cell positionFarthestFromNearestEnemy(int x, int y) {
        Worm nearestEnemyWorm = getNearestEnemyWorm(x, y);
        List<Cell> surroundingCells = getSurroundingCellsNoLava(x, y);
        int[] distanceList = new int[surroundingCells.size()];
        int maxDistance = -999;
        int returnId = -1; // check lagi ntar
        for (int i = 0; i < surroundingCells.size(); i++) {
            Cell block = surroundingCells.get(i);
            distanceList[i] = euclideanDistance(block.x, block.y, nearestEnemyWorm.position.x, nearestEnemyWorm.position.y);
            if (distanceList[i] > maxDistance) {
                maxDistance = distanceList[i];
                returnId = i;
            }
        }
        return (surroundingCells.get(returnId));
    }

    private Cell positionNearestToCenter(int x, int y) {
        // Center dari map adalah x = 17, y = 17
        return (positionNearestToXY(x, y, 17, 17));
    }
    
    private Worm getOpponentByProfession(int id)
    {
        for (int i = 0; i < 3; i++) {
            if (opponent.worms[i].id == id && opponent.worms[i].health > 0) {
                return opponent.worms[i];
            }
        }
        return null;
    }
    private Worm getOpponentByLowestHp(int id)
    {
        int idxlowestHp = -1;
        int LowestHp = 999;
        for (int i = 0; i < 3; i++) {
            if ((opponent.worms[i].health < LowestHp) && (opponent.worms[i].health > 0))
            {
                LowestHp = opponent.worms[i].health;
                idxlowestHp = i;
            }
        }
        if (idxlowestHp == -1) {
            return null;
        } else {
            return opponent.worms[idxlowestHp];
        }
    }  

    private Cell positionNearestToXY(int xWorm, int yWorm, int xTarget, int yTarget) {
        List<Cell> surroundingCells = getSurroundingCells(xWorm, yWorm);
        int[] distanceList = new int[surroundingCells.size()];
        int returnId = -1;
        int minDistance = 999;
        for (int i = 0; i < surroundingCells.size(); i++) {
            Cell block = surroundingCells.get(i);
            distanceList[i] = euclideanDistance(block.x, block.y, xTarget, yTarget);
            if (distanceList[i] < minDistance) {
                minDistance = distanceList[i];
                returnId = i;
            }
        }
        return (surroundingCells.get(returnId));
    }

    private Cell positionFurthestFromXY(int xWorm, int yWorm, int xTarget, int yTarget) {
        List<Cell> surroundingCells = getSurroundingCellsNoLava(xWorm,yWorm); // Menggunakan yang no Lava
        int[] distanceList = new int[surroundingCells.size()];
        int returnId = -1;
        int maxDistance = -999;
        for (int i = 0; i < surroundingCells.size(); i++) {
            Cell block = surroundingCells.get(i);
            distanceList[i] = euclideanDistance(block.x, block.y, xTarget, yTarget);
            if (distanceList[i] > maxDistance) {
                maxDistance = distanceList[i];
                returnId = i;
            }
        }
        return (surroundingCells.get(returnId));
    }

    private Command MoveOrDig(Cell block) {
        if (block.type == CellType.AIR || block.type == CellType.LAVA) {
            return new MoveCommand(block.x, block.y);
        }
        else if (block.type == CellType.DIRT) {
            return new DigCommand(block.x, block.y);
        }
        else {
            return new DoNothingCommand();
        }
    }
}

// Strategi Greedy:
// 3 Worm sejak awal berkumpul di tengah, sekaligus men-dig
// Mencari worm yang sendirian
// Jika tidak bertemu worm yang sendirian, fokus untuk men-dig dan mengambil poin terbanyak
// Jika ditemukan worm yang sendirian maka tiga worm datang untuk menyerang
// 1: Commando, 2: Agent, 3: Technologist



// From ^^
// List<Cell> surroundingCells = getSurroundingCellsNoLava(x, y);
//         int[] distanceList = new int[surroundingCells.size()];
//         int returnId = -1;
//         int minDistance = 999;
//         for (int i = 0; i < surroundingCells.size(); i++) {
//             Cell block = surroundingCells.get(i);
//             distanceList[i] = euclideanDistance(block.x, block.y, 17, 17);
//             if (distanceList[i] < minDistance)
//             {
//                 minDistance = distanceList[i];
//                 returnId =i;
//             }
//         }
//         return (surroundingCells.get(returnId));


// GetAllWormsInRange (method buat nyari worms dengan hp terkecil)
// Benerin Range snowball & banana bomb (range 5 / bisa lewat dirt)
// Ambil power up habis berantem, berarti pas GetAllWormsInRange = null nanti aja dipikirin
// Agent menghindari commando
// GetFriendsInRange (method buat nyari worms friends)
// ConstructFireDirectionLines untuk musuh, target yang didekati adalah ujung dari contstruct fire direction lines