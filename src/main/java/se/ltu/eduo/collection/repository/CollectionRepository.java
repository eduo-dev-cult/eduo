package se.ltu.eduo.collection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.collection.model.Collection;

import java.util.List;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    List<Collection> findAllByOwnerId(Integer ownerId);
}