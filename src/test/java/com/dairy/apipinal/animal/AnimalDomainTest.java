package com.dairy.apipinal.animal;

import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnimalDomainTest {

    @Test
    void newAnimalShouldBeActive() {

        Animal animal = new Animal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DI001",
                "Diouma",
                null,
                null,
                UUID.randomUUID()
        );

        assertEquals(StatutAnimal.ACTIF, animal.getStatut());
    }

    @Test
    void soldAnimalShouldNotBeEditable() {

        Animal animal = new Animal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DI001",
                "Diouma",
                null,
                null,
                UUID.randomUUID()
        );

        animal.changeStatus(
                StatutAnimal.VENDU,
                UUID.randomUUID()
        );

        assertThrows(
                IllegalStateException.class,
                () -> animal.update(
                        UUID.randomUUID(),
                        "DI002",
                        "Diouma",
                        null,
                        null,
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void deadAnimalShouldNotBeEditable() {

        Animal animal = new Animal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DI001",
                "Diouma",
                null,
                null,
                UUID.randomUUID()
        );

        animal.changeStatus(
                StatutAnimal.DECEDE,
                UUID.randomUUID()
        );

        assertThrows(
                IllegalStateException.class,
                () -> animal.update(
                        UUID.randomUUID(),
                        "DI002",
                        "Diouma",
                        null,
                        null,
                        UUID.randomUUID()
                )
        );
    }
}