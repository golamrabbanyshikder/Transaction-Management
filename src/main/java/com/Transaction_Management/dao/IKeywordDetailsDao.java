package com.Transaction_Management.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Transaction_Management.model.KeywordDetailsModel;

@Repository
public interface IKeywordDetailsDao extends JpaRepository<KeywordDetailsModel, Long> {

	boolean existsByKeyword(String keyword);

}
