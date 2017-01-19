package playerone;

import battlecode.common.*;

import java.util.Map;


public strictfp class RobotPlayer {
    static RobotController rc;


    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        ///
        playerone.RobotPlayer.rc = rc;
        System.out.println("I am a " + rc.getType());

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
            case TANK:
                runTank();
                break;
            case SCOUT:
                runScout();
                break;
        }
    }

    private static void runArchon() {
        //Initial variables
        final int MAXPOP  =  25; // max amount that an archon will span
        final int SPAWN_UPDATE_THRESH = 1000; //min amount to prompt the archon to spawn more gardeners
        final int TIME_ABOVE_LIMIT  = 10; //number of rounds the bullet count
                                            // has to be above the limit to restart spawing
        final int SPAWN_INCREASE_COUNT  = 10; //how many more gardners get added to needed amount when spawning restarts

        int numMade  = 0; //gardeners spawned by the archon
        int currentlyNeeded = 5; // current amount needed to make
        int roundsAboveLimit = 0; //tracks the number of rounds above the spawn threshold


        while(true) {
            try {

                //Variables updated every round
                float numBullets = rc.getTeamBullets();
                float victoryPoints  = rc.getTeamVictoryPoints();
                float bulletsNeededToWin = (1000 - rc.getTeamVictoryPoints())*10;

                //if we have enough bullets to get all the victory points we need, donate them and win
                if(numBullets >= bulletsNeededToWin)
                    rc.donate(bulletsNeededToWin);

                //pick a random direction every round and move that way
                //TODO: make the archon hide behind the wall of gardeners
                Direction dir = randomDirection();
                if(rc.canMove(dir)){
                    rc.move(dir);
                }


                //if we have 5000 bullets donate 25000 (250 victory points and 1/4 total needed)
                //TODO: adjust conditonal never triggers with current spwan patterns
                if(rc.getTeamBullets() >= 5000)
                {
                    rc.donate(2500);
                }

                //tracks rounds above the threshold
                if(numBullets > SPAWN_UPDATE_THRESH)
                    roundsAboveLimit++;

                //restarts spawning if:
                //1. we have been above the threshold bullet amounts for long enough
                //2. we don't have any more to make currently
                //3. we haven't made the max amount yet
                if(roundsAboveLimit > TIME_ABOVE_LIMIT && currentlyNeeded <= 0 && numMade < MAXPOP)
                    currentlyNeeded += SPAWN_INCREASE_COUNT;


                //Hire a gardner if we haven't made more then needed
                if (rc.canHireGardener(dir) && currentlyNeeded > 0) {
                    rc.hireGardener(dir);
                    numMade++;
                    currentlyNeeded--;
                }

                //done with tasks, yield turn
                Clock.yield();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void  runGardener() {
        //Initial Variables
        boolean madeFarm = false; //has the gardener made planted a tree yet
        MapLocation start = rc.getLocation(); // initial spawn
        Direction dir = randomDirection();
        boolean madeASoldier = false;

        while(true) {
            try {
                if(!madeFarm){
                    if(rc.getLocation().distanceTo(start) < 15 && !rc.hasMoved()) {
                        while (!rc.hasMoved()) {
                            if (rc.canMove(dir, rc.getType().strideRadius)) {
                                rc.move(dir);
                            }
                            else{
                                dir = randomDirection();
                            }
                        }
                    }

                    else{
                        while(!madeFarm ) {
                            Direction plant = new Direction(rc.getLocation(), start);
                            if (rc.canPlantTree(plant)) {
                                rc.plantTree(plant);
                                madeFarm = true;
                            }
                            else
                            {
                                for(int i = 1; i < 10; i++)
                                {
                                    if (rc.canPlantTree(plant.rotateLeftDegrees(i*2))) {
                                        rc.plantTree(plant.rotateLeftDegrees(i*2));
                                        madeFarm = true;
                                        break;
                                    }
                                    else if (rc.canPlantTree(plant.rotateRightDegrees(i*2))) {
                                        rc.plantTree(plant.rotateRightDegrees(i*2));
                                        madeFarm = true;
                                    }
                                }
                            }
                        }
                    }
                }
                else{
                    TreeInfo[] nearTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, rc.getTeam());
                    if(nearTrees[0].getHealth() < 45){
                        if(rc.canWater(nearTrees[0].getLocation()))
                            rc.water(nearTrees[0].getLocation());
                        if(rc.canShake(nearTrees[0].getLocation()))
                            rc.shake(nearTrees[0].getLocation());
                    }
                    else{
                        if(!madeASoldier) {
                            for (int i = 0; i < 3; i++) {
                                if (rc.canBuildRobot(RobotType.SOLDIER,
                                        new Direction(rc.getLocation(),
                                                nearTrees[0].getLocation()).rotateRightDegrees((i + 1) * 45))) {
                                    rc.buildRobot(RobotType.SOLDIER,
                                            new Direction(rc.getLocation(),
                                                    nearTrees[0].getLocation()).rotateRightDegrees((i + 1) * 45));
                                    madeASoldier = true;
                                }
                                else
                                    for (int j = -10; j < 10; j++) {
                                        if (rc.canBuildRobot(RobotType.SOLDIER,
                                                new Direction(rc.getLocation(),
                                                        nearTrees[0].getLocation()).rotateRightDegrees(((i + 1) * 45) + j))) {
                                            rc.buildRobot(RobotType.SOLDIER,
                                                    new Direction(rc.getLocation(),
                                                            nearTrees[0].getLocation()).rotateRightDegrees(((i + 1) * 45) + j));
                                            madeASoldier = true;
                                            break;
                                        }
                                    }
                            }
                        }
                        else {
                            for (int i = 0; i < 3; i++) {
                                if (rc.canBuildRobot(RobotType.LUMBERJACK,
                                        new Direction(rc.getLocation(),
                                                nearTrees[0].getLocation()).rotateRightDegrees((i + 1) * 45))) {
                                    rc.buildRobot(RobotType.LUMBERJACK,
                                            new Direction(rc.getLocation(),
                                                    nearTrees[0].getLocation()).rotateRightDegrees((i + 1) * 45));
                                    madeASoldier = false;
                                }
                                else
                                    for (int j = -10; j < 10; j++) {
                                        if (rc.canBuildRobot(RobotType.LUMBERJACK,
                                                new Direction(rc.getLocation(),
                                                        nearTrees[0].getLocation()).rotateRightDegrees(((i + 1) * 45) + j))) {
                                            rc.buildRobot(RobotType.LUMBERJACK,
                                                    new Direction(rc.getLocation(),
                                                            nearTrees[0].getLocation()).rotateRightDegrees(((i + 1) * 45) + j));
                                            madeASoldier = false;
                                            break;
                                        }
                                    }
                            }
                        }


                    }
                }

                Clock.yield();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void  runSoldier() {
        //Initial variables
        MapLocation start = rc.getLocation(); //spawn location
        MapLocation teamAvg = averageLocation(rc.getInitialArchonLocations(rc.getTeam()));//avg starting position of teams
                                                                                            //archons
        MapLocation enemyAvg = averageLocation(rc.getInitialArchonLocations(rc.getTeam().opponent()));//avg staring position
                                                                                                        //of enemies archons
        Direction dir = weightedRandomDirection(new Direction(enemyAvg, teamAvg)); //direction in the general direction
                                                                                    //of the enemy


        while(true) {
            try {

                if(rc.getLocation().distanceTo(start) < 5)
                {
                    tryMove(dir);
                }

                RobotInfo[] closeEnemy = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
                if(closeEnemy.length != 0)
                {
                    if(rc.canFireSingleShot())
                        rc.fireSingleShot(new Direction(rc.getLocation(),
                                closeEnemy[((int)(Math.random()*closeEnemy.length))].getLocation()));
                }
                Clock.yield();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void  runLumberjack() {
        String type = "";
        int objectID = 0;
        RobotInfo objectiveRobot = new RobotInfo(0, rc.getTeam(), rc.getType(), new MapLocation(0,0), 0, 0 ,0);
        TreeInfo objectiveTree = new TreeInfo(0, rc.getTeam(), new MapLocation(0,0), 0, 0, 0, rc.getType());
        float distanceTo = 0;
        boolean isObjectivePicked = false;
        boolean isObjectiveComplete = false;
        while(true) {
            try {
                System.out.println("Senseing");
                RobotInfo[] nearRobots = rc.senseNearbyRobots(rc.getType().sensorRadius, rc.getTeam().opponent());
                TreeInfo[] nearTrees = rc.senseNearbyTrees(rc.getType().sensorRadius, Team.NEUTRAL);
                if(!isObjectivePicked)
                {
                    isObjectiveComplete = false;
                    System.out.println("PICKING OBJECTIVE");

                    if(nearRobots.length == 0 && nearTrees.length == 0)
                    {
                        type = "EXPLORE";
                        isObjectivePicked = true;
                    }

                    else {

                        RobotInfo closestRobot = new RobotInfo(0, rc.getTeam(), rc.getType(), new MapLocation(0,0), 0,0,0 );
                        TreeInfo closestTree = new TreeInfo(0,rc.getTeam(), new MapLocation(0,0), 0,0,0, rc.getType());

                        if(nearRobots.length == 0) {
                            type = "TREE";
                            closestTree = findClosest(nearTrees);
                        }
                        else if(nearTrees.length == 0) {
                            type = "ROBOTS";
                            closestRobot = findClosest(nearRobots);
                        }
                        else {
                            type = "NA";
                            closestRobot = findClosest(nearRobots);
                            System.out.println("Closest Robot" + closestRobot.getID());
                            closestTree = findClosest(nearTrees);
                            System.out.println("Closet Tree" + closestTree.getID());
                        }


                        if (rc.getLocation().distanceTo(closestRobot.getLocation()) < rc.getLocation().distanceTo(closestTree.getLocation()) || type == "ROBOT") {
                            type = "ROBOT";
                            objectiveRobot = closestRobot;
                            objectID = objectiveRobot.getID();
                            isObjectivePicked = true;
                            System.out.println("ROBOT CHOSEN");
                        } else if (rc.getLocation().distanceTo(closestRobot.getLocation()) > rc.getLocation().distanceTo(closestTree.getLocation()) || type == "TREE"){
                            type = "TREE";
                            objectiveTree = closestTree;
                            objectID = objectiveTree.getID();
                            isObjectivePicked = true;
                            System.out.println("TREE CHOSEN");
                        }
                    }
                }
                if(!isObjectiveComplete)
                {
                    if(type == "EXPLORE")
                    {
                        Direction rand = randomDirection();
                        tryMove(rand);
                        isObjectiveComplete = true;
                    }

                    else if(type == "ROBOT")
                    {
                        if(objectiveRobot.getHealth() < 0)
                        {
                            isObjectiveComplete = true;
                        }
                        objectiveRobot = updateInfo(nearRobots, objectID);
                        if(objectiveRobot.getID() == 1000001)
                        {
                            System.out.println("OBJECTIVE LOST");
                            isObjectiveComplete = true;
                        }
                        distanceTo = rc.getLocation().distanceTo(objectiveRobot.getLocation());
                        if(rc.canStrike())
                        {
                            rc.strike();
                        }
                        System.out.println("TRYING TO MOVE");

                        tryMove(rc.getLocation().directionTo(objectiveRobot.getLocation()));
                    }
                    else {
                        if(objectiveTree.getHealth() < 0)
                        {
                            isObjectiveComplete = true;
                        }
                        objectiveTree = updateInfo(nearTrees, objectID);
                        if(objectiveTree.getID() == 1000001)
                        {
                            System.out.println("OBJECTIVE LOST");
                            isObjectiveComplete = true;
                        }
                        distanceTo = rc.getLocation().distanceTo(objectiveTree.getLocation());
                        if(distanceTo <= rc.getType().bodyRadius + objectiveTree.radius + rc.getType().strideRadius)
                        {
                            if(rc.canShake(objectiveTree.getLocation()) && objectiveTree.getContainedBullets() > 0)
                            {
                                System.out.println("SHAKING!!!!!");
                                System.out.println(rc.getTeamBullets());
                                rc.shake(objectiveTree.getLocation());
                                System.out.println(rc.getTeamBullets());

                            }
                            else if(rc.canChop(objectiveTree.getLocation()) && objectiveTree.getHealth() > 0){
                                rc.chop(objectiveTree.getLocation());
                                System.out.println("CHOPPING!!");
                            }
                        }

                        System.out.println("TRYING TO MOVE");
                        tryMove(rc.getLocation().directionTo(objectiveTree.getLocation()));
                    }
                }

                if(isObjectiveComplete) {
                    isObjectivePicked = false;
                    isObjectiveComplete = false;
                }
                Clock.yield();
            }
            catch (Exception e) {
            }
            System.out.println("");
        }
    }


    private static void  runTank() {
        while(true) {
            try {
            }
            catch (Exception e) {
            }
        }
    }

    private static void  runScout() {
        while(true) {
            try {
            }
            catch (Exception e) {
            }
        }
    }

    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {

            if(Math.random() < .5) {
                // Try the offset of the left side
                if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
                    rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                    return true;
                }
                // Try the offset on the right side
                if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
                    rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
                    return true;
                }
                // No move performed, try slightly further
                currentCheck++;
            }
            else
            {
                // Try the offset on the right side
                if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
                    rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
                    return true;
                }
                // Try the offset of the left side
                if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
                    rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                    return true;
                }

                // No move performed, try slightly further
                currentCheck++;
            }
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    static TreeInfo findClosest(TreeInfo[] a)
    {
        int index = 0;
        float curr = rc.getLocation().distanceTo(a[0].getLocation());
        float min = curr;
        for(int i = 1; i < a.length; i++) {
            curr = rc.getLocation().distanceTo(a[i].getLocation());
            if (curr < min) {
                min = curr;
                index = i;
            }

        }
        return a[index];
    }



    static RobotInfo findClosest(RobotInfo[] a)
    {
        int index = 0;
        float curr = rc.getLocation().distanceTo(a[0].getLocation());
        float min = curr;
        for(int i = 1; i < a.length; i++) {
            curr = rc.getLocation().distanceTo(a[i].getLocation());
            if (curr < min) {
                min = curr;
                index = i;
            }

        }
        return a[index];
    }

    static RobotInfo updateInfo(RobotInfo[] a, int ID)
    {
        for(int i = 0; i < a.length; i++)
        {
            if(a[i].getID() == ID)
                return a[i];
        }

        return new RobotInfo(1000001, rc.getTeam(), rc.getType(), new MapLocation(0,0), 0, 0 , 0);

    }

    static TreeInfo updateInfo(TreeInfo[] a, int ID)
    {
        for(int i = 0; i < a.length; i++)
        {
            if(a[i].getID() == ID)
                return a[i];
        }

        return new TreeInfo(1000001, rc.getTeam(), new MapLocation(0,0), 0, 0 , 0,rc.getType());
    }

    static MapLocation averageLocation(MapLocation[] a)
    {
        float xSum = 0;
        float ySum = 0;
        for(int i = 0 ; i < a.length; i++)
        {
            xSum += a[i].x;
            ySum += a[i].y;
        }
        xSum /= a.length;
        ySum /= a.length;

        return new MapLocation(xSum, ySum);
    }

    static Direction weightedRandomDirection(Direction dir, int num, float tolerance)
    {
        Direction[] seeds = new Direction[num];
        int i = 0;
        while (i < num)
        {
            Direction rand = randomDirection();
            if(dir.degreesBetween(rand) < tolerance)
            {
                seeds[i] = rand;
                i++;
            }

        }

        float degrees = 0;
        for(int j = 0; j < seeds.length; j++)
        {
            degrees += seeds[j].getAngleDegrees();
        }
        degrees /= num;

        float radians = degrees * (float)Math.PI / 180;
        return new Direction(radians);
    }

    static Direction weightedRandomDirection(Direction dir) {
        return weightedRandomDirection(dir, 5, 80);
    }
}