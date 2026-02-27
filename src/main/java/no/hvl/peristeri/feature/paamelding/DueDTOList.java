package no.hvl.peristeri.feature.paamelding;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class DueDTOList {
    private final List<DueDTO> liste;

    public DueDTOList() {
        liste = new ArrayList<>();
    }

    public DueDTO findDueById(Integer id) {
        for (DueDTO due : liste) {
            if (due.radId().equals(id)) {
                return due;
            }
        }
        return null;
    }

    public void leggTilDueDTO(DueDTO dueDTO) {
        liste.add(dueDTO);
    }

    public void endreDue(DueDTO due) {
        for (int i = 0; i < liste.size(); i++) {
            if (due.radId().equals(liste.get(i).radId())) {
                liste.set(i, due);
                return;
            }
        }
    }

}
