package no.hvl.peristeri.feature.due;

public class DueNotFoundException extends RuntimeException{
    public DueNotFoundException(Long dueId) {
        super("Fant ikke due med id: " + dueId);
    }
}
