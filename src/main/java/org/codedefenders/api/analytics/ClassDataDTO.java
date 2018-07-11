package org.codedefenders.api.analytics;

public class ClassDataDTO {
    private long id;
    private String classname;
    private int nrGames;
    private int nrPlayers;
    private int testsSubmitted;
    private int mutantsSubmitted;
    private int mutantsAlive;
    private int mutantsEquivalent;

    private ClassRatings ratings;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public int getNrGames() {
        return nrGames;
    }

    public void setNrGames(int nrGames) {
        this.nrGames = nrGames;
    }

    public int getNrPlayers() {
        return nrPlayers;
    }

    public void setNrPlayers(int nrPlayers) {
        this.nrPlayers = nrPlayers;
    }

    public int getTestsSubmitted() {
        return testsSubmitted;
    }

    public void setTestsSubmitted(int testsSubmitted) {
        this.testsSubmitted = testsSubmitted;
    }

    public int getMutantsSubmitted() {
        return mutantsSubmitted;
    }

    public void setMutantsSubmitted(int mutantsSubmitted) {
        this.mutantsSubmitted = mutantsSubmitted;
    }

    public int getMutantsAlive() {
        return mutantsAlive;
    }

    public void setMutantsAlive(int mutantsAlive) {
        this.mutantsAlive = mutantsAlive;
    }

    public int getMutantsEquivalent() {
        return mutantsEquivalent;
    }

    public void setMutantsEquivalent(int mutantsEquivalent) {
        this.mutantsEquivalent = mutantsEquivalent;
    }

    public ClassRatings getRatings() {
        return ratings;
    }

    public void setRatings(ClassRatings ratings) {
        this.ratings = ratings;
    }

    public static class ClassRatings {
        private ClassRating cutMutationDifficulty;
        private ClassRating cutTestDifficulty;
        private ClassRating gameEngaging;

        public ClassRating getCutMutationDifficulty() {
            return cutMutationDifficulty;
        }

        public void setCutMutationDifficulty(ClassRating cutMutationDifficulty) {
            this.cutMutationDifficulty = cutMutationDifficulty;
        }

        public ClassRating getCutTestDifficulty() {
            return cutTestDifficulty;
        }

        public void setCutTestDifficulty(ClassRating cutTestDifficulty) {
            this.cutTestDifficulty = cutTestDifficulty;
        }

        public ClassRating getGameEngaging() {
            return gameEngaging;
        }

        public void setGameEngaging(ClassRating gameEngaging) {
            this.gameEngaging = gameEngaging;
        }
    }

    public static class ClassRating {
        private int count;
        private int sum;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getSum() {
            return sum;
        }

        public void setSum(int sum) {
            this.sum = sum;
        }
    }
}
