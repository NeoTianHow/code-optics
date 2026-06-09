// package com.psa.capstone.be;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// import java.net.URI;
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.function.Function;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.Mock;
// import org.mockito.invocation.InvocationOnMock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.quality.Strictness;
// import org.mockito.stubbing.Answer;
// import org.mockito.junit.jupiter.MockitoSettings;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.reactive.function.client.WebClient;
// import
// org.springframework.web.reactive.function.client.WebClientResponseException;
// import org.springframework.web.util.DefaultUriBuilderFactory;
// import org.springframework.web.util.UriBuilder;
// import org.springframework.web.util.UriBuilderFactory;

// import com.jayway.jsonpath.internal.function.sequence.Last;
// import com.psa.capstone.be.dto.BranchDetailsDTO;
// import com.psa.capstone.be.dto.PaginatedResponseDTO;
// import com.psa.capstone.be.dto.ProjectDetailsDTO;
// import com.psa.capstone.be.exception.GitLabException;
// import com.psa.capstone.be.service.BranchComparisonAIService;
// import com.psa.capstone.be.service.BranchService;
// import com.psa.capstone.be.service.BranchSummaryService;
// import com.psa.capstone.be.service.ProjectService;

// import reactor.core.publisher.Flux;
// import reactor.core.publisher.Mono;
// import reactor.test.StepVerifier;

// import org.springframework.web.util.UriBuilder;
// import org.springframework.web.util.UriBuilderFactory;
// import org.springframework.web.util.DefaultUriBuilderFactory;
// import java.net.URI;
// import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
// @MockitoSettings(strictness = Strictness.LENIENT) // Use lenient mode to
// avoid unnecessary stubbing exceptions
// public class ProjectServiceTest {

// @Mock
// private WebClient gitlabWebClient;

// @Mock
// private BranchService branchService;

// @Mock
// private BranchSummaryService branchSummaryService;

// @Mock
// private BranchComparisonAIService qwenAIService;

// private ProjectService projectService;

// @BeforeEach
// void setUp() {
// projectService = new ProjectService(
// gitlabWebClient,
// qwenAIService,
// branchSummaryService,
// branchService);
// }

// @Test
// void testGetSubgroupProjects_Basic() {
// // Set up test parameters
// Long subgroupId = 123L;
// int page = 1;
// int size = 10;
// String searchTerm = "test";

// // Create test data
// LocalDateTime now = LocalDateTime.now();
// List<ProjectDetailsDTO> projects = Arrays.asList(
// createTestProject(1L, "Test Project 1"),
// createTestProject(2L, "Test Project 2"));

// // Set up HTTP headers with pagination info
// HttpHeaders headers = new HttpHeaders();
// headers.add("X-Total", "25");
// ResponseEntity<List<ProjectDetailsDTO>> responseEntity = new
// ResponseEntity<>(projects, headers, HttpStatus.OK);

// // Create test branch data
// List<BranchDetailsDTO> project1Branches = Arrays.asList(
// createTestBranch("main", true, true, true, now.minusDays(10)),
// createTestBranch("feature", false, true, false, now.minusDays(2)));

// // Create WebClient mocks
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// // 1. Set up main WebClient chain
// doReturn(uriSpec).when(gitlabWebClient).get();
// doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
// doReturn(responseSpec).when(headersSpec).retrieve();
// doReturn(Mono.just(responseEntity)).when(responseSpec).toEntityList(eq(ProjectDetailsDTO.class));

// // 2. Mock branch service for any project/branch combination
// doReturn(true).when(branchService).isBranchMerged(anyLong(), anyString(),
// anyString());

// // 3. Simple mock for branch service
// doAnswer(invocation -> {
// // Return different branch lists based on project ID
// Long projectId = invocation.getArgument(0);
// if (projectId.equals(1L)) {
// return project1Branches;
// } else {
// // For any other project ID, return a simple list with just a main branch
// return List.of(createTestBranch("main", true, true, true, now.minusDays(5)));
// }
// }).when(branchService).getAllProjectBranches(anyLong(), anyString());

// // 4. For bodyToFlux, use a simpler approach
// doReturn(Flux.empty()).when(responseSpec).bodyToFlux(eq(BranchDetailsDTO.class));

// // Call the method being tested
// Mono<PaginatedResponseDTO<ProjectDetailsDTO>> resultMono =
// projectService.getSubgroupProjects(subgroupId, page,
// size, searchTerm);

// // Get the result
// PaginatedResponseDTO<ProjectDetailsDTO> result = resultMono.block();

// // Basic assertions
// assertNotNull(result, "Result should not be null");
// assertEquals(2, result.getItems().size(), "Should return 2 projects");
// assertEquals(25, result.getTotalItems(), "Total count should be 25");
// assertEquals(page, result.getCurrentPage(), "Page should match input");
// assertEquals(size, result.getPageSize(), "Size should match input");
// }

// @Test
// void testGetSubgroupProjects_ErrorHandling() {
// // Set up test parameters
// Long subgroupId = 123L;
// int page = 1;
// int size = 10;
// String searchTerm = "test";

// // Create WebClient mocks
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// // Set up WebClient to throw an exception
// doReturn(uriSpec).when(gitlabWebClient).get();
// doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
// doReturn(responseSpec).when(headersSpec).retrieve();
// doReturn(Mono.error(new WebClientResponseException(500, "Server Error", null,
// null, null)))
// .when(responseSpec).toEntityList(eq(ProjectDetailsDTO.class));

// // Call the method being tested
// Mono<PaginatedResponseDTO<ProjectDetailsDTO>> resultMono =
// projectService.getSubgroupProjects(subgroupId, page,
// size, searchTerm);

// // Verify that the error is propagated as a GitLabException
// StepVerifier.create(resultMono)
// .expectError(GitLabException.class)
// .verify();
// }

// @Test
// void testEnrichProjectDetailsAsync_Success() {
// // Create a test project
// ProjectDetailsDTO project = createTestProject(1L, "Test Project");

// // Create test branches
// LocalDateTime now = LocalDateTime.now();
// List<BranchDetailsDTO> branches = Arrays.asList(
// createTestBranch("main", true, true, true, now.minusDays(5)),
// createTestBranch("feature", false, false, false, now.minusDays(1)));

// // Create WebClient mocks
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// // Set up WebClient to return branches
// doReturn(uriSpec).when(gitlabWebClient).get();
// doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
// doReturn(responseSpec).when(headersSpec).retrieve();
// doReturn(Flux.fromIterable(branches)).when(responseSpec).bodyToFlux(eq(BranchDetailsDTO.class));

// // Mock branch service
// doReturn(true).when(branchService).isBranchMerged(eq(1L), eq("main"),
// eq("main"));
// doReturn(false).when(branchService).isBranchMerged(eq(1L), eq("feature"),
// eq("main"));

// // Access the private method using reflection
// java.lang.reflect.Method method;
// try {
// method = ProjectService.class.getDeclaredMethod("enrichProjectDetailsAsync",
// ProjectDetailsDTO.class);
// method.setAccessible(true);

// // Call the method
// Mono<ProjectDetailsDTO> result = (Mono<ProjectDetailsDTO>)
// method.invoke(projectService, project);

// // Verify results
// ProjectDetailsDTO enrichedProject = result.block();
// assertNotNull(enrichedProject);
// assertEquals(2, enrichedProject.getActiveBranches(), "Should have 2 active
// branches");
// assertEquals(0, enrichedProject.getStaleBranches(), "Should have 0 stale
// branches");
// assertEquals(1, enrichedProject.getMergedBranches(), "Should have 1 merged
// branch");
// assertEquals(1, enrichedProject.getUnmergedBranches(), "Should have 1
// unmerged branch");
// assertEquals("feature", enrichedProject.getMostRecentBranchName(), "Most
// recent branch should be feature");

// } catch (Exception e) {
// fail("Exception occurred: " + e.getMessage());
// }
// }

// @Test
// void testUpdateProjectMetrics() {
// // Create test project
// ProjectDetailsDTO project = createTestProject(1L, "Test Project");

// // Create test branches with various scenarios
// LocalDateTime now = LocalDateTime.now();
// List<BranchDetailsDTO> branches = new ArrayList<>();

// // 1. Active and merged branch
// branches.add(createTestBranch("main", true, true, true, now.minusDays(5)));

// // 2. Active and unmerged branch
// branches.add(createTestBranch("feature1", false, true, false,
// now.minusDays(1)));

// // 3. Stale and merged branch
// branches.add(createTestBranch("old-feature", false, false, true,
// now.minusDays(40)));

// // 4. Stale and unmerged branch
// branches.add(createTestBranch("abandoned", false, false, false,
// now.minusDays(60)));

// // 5. Branch with null commit (edge case)
// BranchDetailsDTO nullCommitBranch = new BranchDetailsDTO();
// nullCommitBranch.setBranchName("null-commit");
// nullCommitBranch.setMerged(false);
// branches.add(nullCommitBranch);

// // Call the method directly
// projectService.updateProjectMetrics(project, branches);

// // Verify metrics
// assertEquals(2, project.getActiveBranches(), "Should have 2 active
// branches");
// assertEquals(3, project.getStaleBranches(), "Should have 3 stale branches");
// assertEquals(2, project.getMergedBranches(), "Should have 2 merged
// branches");
// assertEquals(3, project.getUnmergedBranches(), "Should have 3 unmerged
// branches");
// assertEquals("feature1", project.getMostRecentBranchName(), "Most recent
// branch should be feature1");
// assertEquals(now.minusDays(1), project.getLastActivityAt(),
// "Last activity date should match most recent commit");
// }

// @Test
// void testUpdateProjectMetrics_EmptyBranchList() {
// // Create test project
// ProjectDetailsDTO project = createTestProject(1L, "Test Project");

// // Create empty branch list
// List<BranchDetailsDTO> branches = new ArrayList<>();

// // Call the method directly
// projectService.updateProjectMetrics(project, branches);

// // Verify metrics
// assertEquals(0, project.getActiveBranches(), "Should have 0 active
// branches");
// assertEquals(0, project.getStaleBranches(), "Should have 0 stale branches");
// assertEquals(0, project.getMergedBranches(), "Should have 0 merged
// branches");
// assertEquals(0, project.getUnmergedBranches(), "Should have 0 unmerged
// branches");
// assertNull(project.getMostRecentBranchName(), "Most recent branch should be
// null");
// assertNull(project.getLastActivityAt(), "Last activity date should be null");
// }

// @Test
// void testEnrichBranchesWithMetrics() {
// // Create test project
// ProjectDetailsDTO project = createTestProject(1L, "Test Project");

// // Create test branches
// LocalDateTime now = LocalDateTime.now();
// List<BranchDetailsDTO> branches = Arrays.asList(
// createTestBranch("main", true, true, true, now.minusDays(5)),
// createTestBranch("feature", false, false, false, now.minusDays(1)));

// // Mock branch service
// doReturn(true).when(branchService).isBranchMerged(eq(1L), eq("main"),
// eq("main"));
// doReturn(false).when(branchService).isBranchMerged(eq(1L), eq("feature"),
// eq("main"));

// // Access the private method using reflection
// java.lang.reflect.Method method;
// try {
// method = ProjectService.class.getDeclaredMethod("enrichBranchesWithMetrics",
// ProjectDetailsDTO.class,
// List.class);
// method.setAccessible(true);

// // Call the method
// @SuppressWarnings("unchecked")
// Mono<ProjectDetailsDTO> result = (Mono<ProjectDetailsDTO>)
// method.invoke(projectService, project, branches);

// // Verify results
// ProjectDetailsDTO enrichedProject = result.block();
// assertNotNull(enrichedProject);
// assertEquals(2, enrichedProject.getActiveBranches(), "Should have 2 active
// branches");
// assertEquals(0, enrichedProject.getStaleBranches(), "Should have 0 stale
// branches");
// assertEquals(1, enrichedProject.getMergedBranches(), "Should have 1 merged
// branch");
// assertEquals(1, enrichedProject.getUnmergedBranches(), "Should have 1
// unmerged branch");

// } catch (Exception e) {
// fail("Exception occurred: " + e.getMessage());
// }
// }

// /**
// * Creates a test project with specified ID and name
// */
// private ProjectDetailsDTO createTestProject(Long id, String name) {
// ProjectDetailsDTO project = new ProjectDetailsDTO();
// project.setId(id);
// project.setProjectName(name);
// project.setWebUrl("https://gitlab.com/test/" + name);
// project.setDefaultBranch("main");
// return project;
// }

// /**
// * Creates a mock commit for branch testing
// */
// private BranchDetailsDTO.Commit createMockCommit(String id, LocalDateTime
// date) {
// BranchDetailsDTO.Commit commit = new BranchDetailsDTO.Commit();
// commit.setId(id);
// commit.setCommittedDate(date);
// commit.setCommitterName("Test User");
// return commit;
// }

// /**
// * Creates a test branch
// */
// private BranchDetailsDTO createTestBranch(String name, boolean isDefault,
// boolean isActive,
// boolean isMerged, LocalDateTime commitDate) {
// BranchDetailsDTO branch = new BranchDetailsDTO();
// branch.setBranchName(name);
// branch.setDefault(isDefault);
// branch.setActive(isActive);
// branch.setMerged(isMerged);
// branch.setWebUrl("https://gitlab.com/test/project/-/tree/" + name);

// BranchDetailsDTO.Commit commit = createMockCommit(
// "commit-" + name,
// commitDate);
// branch.setCommit(commit);

// return branch;
// }

// @Test
// void testGetSubgroupProjects_WithSearchTerm() {
// // Set up parameters with search term
// Long subgroupId = 123L;
// int page = 1;
// int size = 10;
// String searchTerm = "search-test";

// // Mock WebClient for URI building
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// // Set up WebClient to capture URI parameters using doReturn().when() syntax
// doReturn(uriSpec).when(gitlabWebClient).get();
// doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
// doReturn(responseSpec).when(headersSpec).retrieve();

// // Set up empty response
// HttpHeaders headers = new HttpHeaders();
// headers.add("X-Total", "0");
// ResponseEntity<List<ProjectDetailsDTO>> emptyResponse = new
// ResponseEntity<>(new ArrayList<>(), headers,
// HttpStatus.OK);
// doReturn(Mono.just(emptyResponse)).when(responseSpec).toEntityList(eq(ProjectDetailsDTO.class));

// // Call the method
// projectService.getSubgroupProjects(subgroupId, page, size, searchTerm);

// // Verify URI building was called with the search term
// verify(uriSpec).uri(any(Function.class));
// }

// /**
// * Test for error handling in enrichProjectDetailsAsync
// */
// @Test
// void testEnrichProjectDetailsAsync_Error() throws Exception {
// // Create a test project
// ProjectDetailsDTO project = new ProjectDetailsDTO();
// project.setId(1L);
// project.setProjectName("Test Project");

// // Mock WebClient to throw exception
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// doReturn(uriSpec).when(gitlabWebClient).get();
// doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
// doReturn(responseSpec).when(headersSpec).retrieve();
// doReturn(Flux.error(new WebClientResponseException(500, "Server Error", null,
// null, null)))
// .when(responseSpec).bodyToFlux(eq(BranchDetailsDTO.class));

// // Access the private method via reflection
// java.lang.reflect.Method method = ProjectService.class.getDeclaredMethod(
// "enrichProjectDetailsAsync", ProjectDetailsDTO.class);
// method.setAccessible(true);

// // Call the method and verify it handles the error
// Mono<ProjectDetailsDTO> result = (Mono<ProjectDetailsDTO>)
// method.invoke(projectService, project);

// StepVerifier.create(result)
// .expectError(GitLabException.class)
// .verify();
// }

// /**
// * Test for error handling in enrichBranchesWithMetrics
// */
// @Test
// void testEnrichBranchesWithMetrics_Error() throws Exception {
// // Create test project and branches
// ProjectDetailsDTO project = new ProjectDetailsDTO();
// project.setId(1L);
// project.setProjectName("Test Project");
// project.setDefaultBranch("main");

// List<BranchDetailsDTO> branches = new ArrayList<>();
// BranchDetailsDTO branch = new BranchDetailsDTO();
// branch.setBranchName("test-branch");
// branches.add(branch);

// // Mock branch service to throw exception
// doThrow(new RuntimeException("Test exception"))
// .when(branchService).isBranchMerged(anyLong(), anyString(), anyString());

// // Access the private method via reflection
// java.lang.reflect.Method method = ProjectService.class.getDeclaredMethod(
// "enrichBranchesWithMetrics", ProjectDetailsDTO.class, List.class);
// method.setAccessible(true);

// // Call the method and verify it handles the error
// Mono<ProjectDetailsDTO> result = (Mono<ProjectDetailsDTO>)
// method.invoke(projectService, project, branches);

// StepVerifier.create(result)
// .expectError()
// .verify();
// }

// /**
// * Test for timeout handling in getSubgroupProjects
// */
// @Test
// void testGetSubgroupProjects_Timeout() {
// // Set up parameters
// Long subgroupId = 123L;
// int page = 1;
// int size = 10;

// // Mock WebClient to delay beyond timeout
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// doReturn(uriSpec).when(gitlabWebClient).get();
// doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
// doReturn(responseSpec).when(headersSpec).retrieve();

// // Return a Mono that never completes (will trigger timeout)
// // Specify the exact class type to avoid ambiguity
// doReturn(Mono.never()).when(responseSpec).toEntityList(eq(ProjectDetailsDTO.class));

// // Call the method and verify timeout is handled
// Mono<PaginatedResponseDTO<ProjectDetailsDTO>> result =
// projectService.getSubgroupProjects(subgroupId, page,
// size, null);

// StepVerifier.create(result)
// .expectError()
// .verify();
// }

// @Mock
// private UriBuilder uriBuilder;

// /*
// * cover lambda$enrichProjectDetailsAsync$6 (branches URI building)
// * This tests the repository/branches URI path using ArgumentCaptor
// */

// @Test
// void testLambdaEnrichProjectDetailsAsyncUriBuilding() throws Exception {
// // Create a test project
// ProjectDetailsDTO project = new ProjectDetailsDTO();
// project.setId(42L);
// project.setProjectName("Test Project");

// // Create mock objects
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// // Create a captor for the URI path
// ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);

// // Create a stub URI to return
// java.net.URI stubUri = java.net.URI.create("https://example.com/test");

// // Set up capture of URI building function
// doReturn(uriSpec).when(gitlabWebClient).get();
// doAnswer(new Answer<WebClient.RequestHeadersSpec<?>>() {
// @SuppressWarnings("unchecked")
// public WebClient.RequestHeadersSpec<?> answer(InvocationOnMock invocation) {
// Function<UriBuilder, java.net.URI> uriFunction = (Function<UriBuilder,
// java.net.URI>) invocation
// .getArgument(0);

// // Mock the UriBuilder more simply - avoid build() method entirely
// doReturn(uriBuilder).when(uriBuilder).path(pathCaptor.capture());
// doReturn(stubUri).when(uriBuilder).build();

// // Execute the URI function to hit the lambda
// uriFunction.apply(uriBuilder);

// return headersSpec;
// }
// }).when(uriSpec).uri(any(Function.class));

// doReturn(responseSpec).when(headersSpec).retrieve();
// doReturn(Flux.empty()).when(responseSpec).bodyToFlux(eq(BranchDetailsDTO.class));

// // Access private method via reflection
// java.lang.reflect.Method method = ProjectService.class.getDeclaredMethod(
// "enrichProjectDetailsAsync", ProjectDetailsDTO.class);
// method.setAccessible(true);

// // Call the method to execute the lambda
// Mono<ProjectDetailsDTO> result = (Mono<ProjectDetailsDTO>)
// method.invoke(projectService, project);
// result.block(); // Force execution

// // Verify the path was set correctly
// assertEquals("/projects/{id}/repository/branches", pathCaptor.getValue(),
// "URI path should be set to repository branches endpoint");
// }

// /**
// * Test to cover lambda$getSubgroupProjects$1 (error handling)
// * Tests the error handling path when projects can't be fetched
// */
// @Test
// void testLambdaGetSubgroupProjectsErrorHandling() {
// // Set up test parameters
// Long subgroupId = 123L;
// int page = 1;
// int size = 10;

// // Create mock objects with error response
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// doReturn(uriSpec).when(gitlabWebClient).get();
// doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
// doReturn(responseSpec).when(headersSpec).retrieve();

// // Return an error to trigger the onErrorResume lambda
// WebClientResponseException exception = new WebClientResponseException(
// 500, "Server Error", null, null, null);
// doReturn(Mono.error(exception)).when(responseSpec).toEntityList(eq(ProjectDetailsDTO.class));

// // Call the method that will execute the error handling lambda
// Mono<PaginatedResponseDTO<ProjectDetailsDTO>> result =
// projectService.getSubgroupProjects(subgroupId, page,
// size, null);

// // Verify the error is transformed to GitLabException
// StepVerifier.create(result)
// .expectError(GitLabException.class)
// .verify();
// }

// /**
// * Test to specifically cover the URI building lambda in getSubgroupProjects
// */
// @Test
// void testLambdaUriBuilding() {
// // Set up test parameters
// Long subgroupId = 123L;
// int page = 2;
// int size = 15;
// String searchTerm = "test-project";

// // Use a real UriBuilder for better compatibility
// UriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
// UriBuilder realUriBuilder = uriBuilderFactory.builder();

// // Create mock objects
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// // Set up WebClient to use our real UriBuilder
// doReturn(uriSpec).when(gitlabWebClient).get();
// doAnswer(invocation -> {
// Function<UriBuilder, URI> uriFunction = invocation.getArgument(0);
// // This will actually execute the lambda with a real UriBuilder
// URI result = uriFunction.apply(realUriBuilder);
// return headersSpec;
// }).when(uriSpec).uri(any(Function.class));

// doReturn(responseSpec).when(headersSpec).retrieve();

// // Set up response
// HttpHeaders headers = new HttpHeaders();
// headers.add("X-Total", "0");
// ResponseEntity<List<ProjectDetailsDTO>> emptyResponse = new
// ResponseEntity<>(new ArrayList<>(), headers,
// HttpStatus.OK);
// doReturn(Mono.just(emptyResponse)).when(responseSpec).toEntityList((Class<ProjectDetailsDTO>)
// any(Class.class));

// // Call the method to execute the lambda
// projectService.getSubgroupProjects(subgroupId, page, size, searchTerm);
// }

// /**
// * Test to cover enrichProjectDetailsAsync URI building lambda
// */
// @Test
// void testEnrichDetailsUriBuilding() throws Exception {
// // Create a test project
// ProjectDetailsDTO project = new ProjectDetailsDTO();
// project.setId(42L);
// project.setProjectName("Test Project");

// // Use a real UriBuilder
// UriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
// UriBuilder realUriBuilder = uriBuilderFactory.builder();

// // Create mock objects
// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// // Set up WebClient
// doReturn(uriSpec).when(gitlabWebClient).get();
// doAnswer(invocation -> {
// Function<UriBuilder, URI> uriFunction = invocation.getArgument(0);
// // Execute lambda with real builder
// URI result = uriFunction.apply(realUriBuilder);
// return headersSpec;
// }).when(uriSpec).uri(any(Function.class));

// doReturn(responseSpec).when(headersSpec).retrieve();
// doReturn(Flux.empty()).when(responseSpec)
// .bodyToFlux((Class<ProjectDetailsDTO>) any(Class.class));

// // Access private method via reflection
// Method method = ProjectService.class.getDeclaredMethod(
// "enrichProjectDetailsAsync", ProjectDetailsDTO.class);
// method.setAccessible(true);

// // Call the method to execute the lambda
// Mono<ProjectDetailsDTO> result = (Mono<ProjectDetailsDTO>)
// method.invoke(projectService, project);
// result.block(); // Force execution
// }

// @Test
// void testGetSubgroupProjects_WhenEnrichmentFails() {
// Long subgroupId = 123L;
// int page = 1;
// int size = 10;
// String searchTerm = "test";

// // Create test project
// List<ProjectDetailsDTO> projects = List.of(createTestProject(1L, "Project
// 1"));
// HttpHeaders headers = new HttpHeaders();
// headers.add("X-Total", "1");
// ResponseEntity<List<ProjectDetailsDTO>> responseEntity = new
// ResponseEntity<>(projects, headers, HttpStatus.OK);

// WebClient.RequestHeadersUriSpec<?> uriSpec =
// mock(WebClient.RequestHeadersUriSpec.class);
// WebClient.RequestHeadersSpec<?> headersSpec =
// mock(WebClient.RequestHeadersSpec.class);
// WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

// doReturn(uriSpec).when(gitlabWebClient).get();
// doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
// doReturn(responseSpec).when(headersSpec).retrieve();
// doReturn(Mono.just(responseEntity)).when(responseSpec).toEntityList(eq(ProjectDetailsDTO.class));

// // Simulate error during enrichment
// doReturn(uriSpec).when(gitlabWebClient).get();
// doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
// doReturn(responseSpec).when(headersSpec).retrieve();
// doReturn(Flux.error(new RuntimeException("Enrichment failed")))
// .when(responseSpec).bodyToFlux(eq(BranchDetailsDTO.class)); // Fail
// enrichment step

// // Run the method
// Mono<PaginatedResponseDTO<ProjectDetailsDTO>> resultMono =
// projectService.getSubgroupProjects(subgroupId, page,
// size, searchTerm);

// StepVerifier.create(resultMono)
// .expectErrorMatches(throwable -> {
// if (throwable instanceof GitLabException) {
// return throwable.getMessage().equals("Failed to get project details") &&
// throwable.getCause() instanceof GitLabException &&
// throwable.getCause().getMessage()
// .contains("Failed to fetch project details for project 1");
// }
// return false;
// })
// .verify();
// }

// }