package se.ltu.eduo.collection.model;

public enum GenerationFocusArea {
    ENTIRE_MATERIAL("Entire material"),
    KEY_CONCEPTS("Key concepts"),
    TOPICS("Topics: ");

    private final String focusArea;

    GenerationFocusArea(String focusArea) {
        this.focusArea = focusArea;
    }

    public String format(String topics) {
        return this == TOPICS ? focusArea + topics : focusArea;
    }
}