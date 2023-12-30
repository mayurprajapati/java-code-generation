package com.example.helpers;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;

public class GitlabHelper {
	private final String pat;
	private final Long projectId;

	public GitlabHelper(String pat, Long projectId) {
		super();
		this.pat = pat;
		this.projectId = projectId;
	}

	public void createMergeRequest(String sourceBranch, String targetBranch, String title, String description,
			Long assigneeId) throws GitLabApiException {
		try (GitLabApi gitLabApi = new GitLabApi("https://gitlab.com", pat)) {
			gitLabApi.getMergeRequestApi().createMergeRequest(projectId, sourceBranch, targetBranch, title, description,
					assigneeId);
			System.out.println("Merge request was created successfully");
		}
	}
}
