package se.ltu.eduo.collection.model;

public enum GenerationLanguage {
    ENGLISH("English"),
    SWEDISH("Swedish");

    private final String language;

    GenerationLanguage(String language){
        this.language = language;
    }

    public String language(){
        return language;
    }
}
