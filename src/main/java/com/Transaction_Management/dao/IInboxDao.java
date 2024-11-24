package com.Transaction_Management.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Transaction_Management.model.InboxModel;

@Repository
public interface IInboxDao extends JpaRepository<InboxModel, Long> {
	List<InboxModel> findByStatus(String status);
}
