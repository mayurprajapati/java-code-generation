package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.Properties;
import java.util.Random;

import javax.lang.model.element.Modifier;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;

import com.example.helpers.GitlabHelper;
import com.example.utils.PropertiesUtils;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

public class RunMe {
	private static UsernamePasswordCredentialsProvider gitCreds;
	private static String gitlabUsername = "";
	private static String gitlabPersonalAccessToken = "";
	static String branch = "branch" + randomString();
	static String commitMsg = "This is commit message";
	static Properties props = new Properties();
	private static Long gitlabProjectId;
	private static Long gitlabMrAssigneeId;
	private static String mainBranchName;
	private static String gitlabRemoteRepoUrl;
	private static String projectDir = "example-java-project";

	public static void main(String[] args) throws Exception {
		props = PropertiesUtils.loadProps("props.properties");

		gitlabUsername = props.getProperty("gitlabUsername");
		gitlabPersonalAccessToken = props.getProperty("gitlabPersonalAccessToken");
		gitlabProjectId = Long.parseLong(props.getProperty("gitlabProjectId"));
		gitlabMrAssigneeId = Long.parseLong(props.getProperty("gitlabMrAssigneeId"));
		gitlabRemoteRepoUrl = props.getProperty("gitlabRemoteRepoUrl");

		mainBranchName = props.getProperty("mainBranchName");

		gitCreds = new UsernamePasswordCredentialsProvider(gitlabUsername, gitlabPersonalAccessToken);

		String mainJavaCode = generateJava();
		System.out.println("Generated java code: " + mainJavaCode);
		publishJavaCodeOnGitAndCreateMR(mainJavaCode);
	}

	private static void publishJavaCodeOnGitAndCreateMR(String mainJavaCode) throws Exception {
		Git git = gitCloneRepositoryAndCheckoutBranch();

		// delete existing java file
		Path mainJavaPath = Paths.get(projectDir, "src", "main", "java", "com", "example", "Main.java");
		mainJavaPath.toFile().getParentFile().mkdirs();
		mainJavaPath.toFile().delete();

		// write java file
		Files.writeString(mainJavaPath, mainJavaCode, StandardOpenOption.CREATE_NEW);

		if (git.diff().call().isEmpty()) {
			System.out.println("No files to commit");
			return;
		}

		gitCommit(git);

		GitlabHelper gitlab = new GitlabHelper(gitlabPersonalAccessToken, gitlabProjectId);
		gitlab.createMergeRequest(branch, mainBranchName, "Adding sum method", commitMsg, gitlabMrAssigneeId);
	}

	private static String generateJava() {
		String className = "Main";
		Builder builderClass = TypeSpec.classBuilder(className);

		int a = random(0, Integer.MAX_VALUE);
		int b = random(0, Integer.MAX_VALUE);

		MethodSpec methodSpec = buildSumMethod(a, b);
		builderClass.addMethod(methodSpec);

		JavaFile.Builder javaFile = JavaFile.builder("com.example", builderClass.build());

		// convert our work into java source
		return javaFile.build().toString();
	}

	private static int random(int min, int max) {
		Random random = new Random();
		return random.nextInt(max - min) + min;
	}

	private static void gitCommit(Git git)
			throws GitAPIException, NoFilepatternException, NoHeadException, NoMessageException, UnmergedPathsException,
			ConcurrentRefUpdateException, WrongRepositoryStateException, InvalidRemoteException, TransportException {
		git.add().addFilepattern(".").call();
		git.commit().setMessage(commitMsg).call();
		git.push().setCredentialsProvider(gitCreds).call();
		git.getRepository().close();

		System.out.println("Code commit was successful to git");
	}

	private static Git gitCloneRepositoryAndCheckoutBranch()
			throws IOException, GitAPIException, InvalidRemoteException, TransportException, RefAlreadyExistsException,
			RefNotFoundException, InvalidRefNameException, CheckoutConflictException {
		// delete existing cloned repository
		File dir = new File(projectDir);
		if (dir.exists()) {
			FileUtils.delete(dir, FileUtils.RECURSIVE);
		}

		Git git = Git.cloneRepository().setCredentialsProvider(gitCreds).setURI(gitlabRemoteRepoUrl).setDirectory(dir)
				.call();

		git.branchCreate().setName(branch).call();
		git.checkout().setName(branch).call();

		return git;
	}

	private static String randomString() {
		return String.valueOf(Calendar.getInstance().getTimeInMillis());
	}

	private static MethodSpec buildSumMethod(int a, int b) {
		String methodName = String.format("sumMethod%sPlus%s", a, b);

		com.squareup.javapoet.MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

		methodBuilder.returns(TypeName.INT);
		methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);

		// do sum and store it in sum variable
		methodBuilder.addStatement(String.format("int sum = %s + %s", a, b));

		// add return statement
		methodBuilder.addStatement("return sum");

		return methodBuilder.build();
	}
}
