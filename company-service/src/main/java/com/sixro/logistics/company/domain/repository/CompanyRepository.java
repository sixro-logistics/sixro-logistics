package com.sixro.logistics.company.domain.repository;

import com.sixro.logistics.company.domain.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository
        extends JpaRepository<Company, UUID>,
        JpaSpecificationExecutor<Company> {

    Optional<Company> findByCompanyIdAndIsDeletedFalse(UUID companyId);

    boolean existsByBusinessNumberAndIsDeletedFalse(String businessNumber);
}
