package com.mindata.hotel.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSearchRepository extends JpaRepository<SearchJpaEntity, String> {

    long countBySearchHash(String searchHash);
}
