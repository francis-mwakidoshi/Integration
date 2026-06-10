package com.frank.repository;

import com.frank.entity.CountryInfo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryInfoRepository extends JpaRepository<CountryInfo, Long> {

    @EntityGraph(attributePaths = "languages")
    @Override
    List<CountryInfo> findAll();

    @EntityGraph(attributePaths = "languages")
    @Override
    Optional<CountryInfo> findById(Long id);

    Optional<CountryInfo> findByIsoCode(String isoCode);

    boolean existsByIsoCode(String isoCode);
}
