package com.Transaction_Management.dto;

import java.util.List;

public class ApiResponse<T> {

	private List<T> contents;

	public List<T> getContents() {
		return contents;
	}

	public void setContents(List<T> contents) {
		this.contents = contents;
	}

}
