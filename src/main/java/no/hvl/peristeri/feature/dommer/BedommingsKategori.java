package no.hvl.peristeri.feature.dommer;

public enum BedommingsKategori {
    HOLDNING("Holdning"),
    FIGUR("Figur"),
    HODE("Hode"),
    NEBB("Nebb"),
    OYNE("Øyne"),
    HALS_NAKKE("Hals/nakke"),
    VINGER("Vinger"),
    HALE("Hale"),
    BEIN("Bein"),
    FARGE("Farge"),
    TEGNING("Tegning"),
    FJAER_STRUKTUR("Fjær struktur");

    private final String visningsnavn;

    BedommingsKategori(String visningsnavn) {
        this.visningsnavn = visningsnavn;
    }

    public String getVisningsnavn() {
        return visningsnavn;
    }
}

