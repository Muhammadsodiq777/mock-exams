package com.mockexams.backend.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockexams.backend.entity.BandConversion;

public interface BandConversionRepository extends JpaRepository<BandConversion, Long> {

	@Query("""
			select conversion
			from BandConversion conversion
			where conversion.testType = :testType
			  and conversion.minRawScore <= :rawScore
			  and conversion.maxRawScore >= :rawScore
			order by conversion.minRawScore desc
			""")
	List<BandConversion> findMatching(
			@Param("testType") String testType,
			@Param("rawScore") BigDecimal rawScore
	);
}
