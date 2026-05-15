package se.ltu.eduo.user.request;

public record RegisterRequest(String firstName, String lastName, String username, String password) {}