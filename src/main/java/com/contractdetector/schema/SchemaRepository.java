package com.contractdetector.schema;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaRepository extends JpaRepository<ApiSchema, String> {

    Optional<ApiSchema> findFirstByEndpointPathAndHttpMethodOrderByVersionDesc(String endpointPath, String httpMethod);

    List<ApiSchema> findByEndpointPathAndHttpMethodOrderByVersionDesc(String endpointPath, String httpMethod);

    @Query("SELECT COUNT(s) FROM ApiSchema s WHERE s.endpointPath = :endpointPath AND s.httpMethod = :httpMethod")
    long countVersions(String endpointPath, String httpMethod);
}
