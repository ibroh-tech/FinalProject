# Literature notes — LLM-based test generation

Initial notes collected for Week 1 (2026-11-25). Five papers were selected to cover empirical
evidence, industrial deployment, REST API specifics, Java/JUnit generation, and the research
landscape. Notes are based on the abstracts and published metadata; the full texts should be
read before the literature-review chapter is written. Do not cite numbers from these notes
without checking the paper itself.

## 1. TestPilot — empirical evaluation of LLM unit test generation

**Citation:** M. Schäfer, S. Nadi, A. Eghbali, F. Tip, "An Empirical Evaluation of Using Large
Language Models for Automated Unit Test Generation," *IEEE Transactions on Software
Engineering*, vol. 50, no. 1, pp. 85–105, 2024. DOI: 10.1109/TSE.2023.3334955.
arXiv:2302.06527.

**Approach.** TestPilot generates JavaScript unit tests without additional training or manual
effort. The prompt contains the function signature, its implementation, and usage examples
extracted from documentation. Failed tests are re-prompted with the failing test and the error
message (a repair loop). Evaluated with gpt-3.5-turbo on 25 npm packages and 1,684 API
functions.

**Findings.**
- Median statement coverage 70.2% and branch coverage 52.8%, compared with Nessie at 51.3%
  statement and 25.6% branch coverage.
- The older code-cushman-002 model reached 68.2% median statement coverage; StarCoder reached
  54.0%, so model choice affects results but the approach is not tied to one model.
- 92.8% of generated tests had at most 50% similarity to existing tests; none were exact copies.

**Relevance to this project.**
- Supports providing rich context (specification details, schemas, examples) in prompts.
- Motivates a repair/retry step and measuring coverage rather than trusting output.
- Suggests recording the exact model, because different models produce materially different results.

**Limitations / gaps.** JavaScript unit tests, not Java REST integration tests; a single
generation per function; coverage is not the same as fault detection; the repair loop needs a
working execution environment.

## 2. TestGen-LLM at Meta — industrial deployment with quality filters

**Citation:** N. Alshahwan, J. Chheda, A. Finogenova, B. Gokkaya, M. Harman, I. Harper,
A. Marginean, S. Sengupta, E. Wang, "Automated Unit Test Improvement using Large Language
Models at Meta," *32nd ACM Symposium on the Foundations of Software Engineering (FSE '24)*,
Industry Papers, 2024. arXiv:2402.09171.

**Approach.** TestGen-LLM improves existing human-written test classes. Generated tests must
pass filters: they must build, pass reliably, and measurably increase coverage. The paper
reports deployment during Instagram and Facebook test-a-thons.

**Findings.**
- On Instagram Reels and Stories, 75% of generated test cases built correctly, 57% passed
  reliably, and 25% increased coverage.
- 11.5% of classes were improved; 73% of recommendations were accepted for production by
  engineers.

**Relevance to this project.**
- Direct precedent for a validation pipeline with explicit quality gates before human review.
- "Measurably improve coverage" is a useful acceptance criterion for generated tests.
- The gap between 75% building and 57% passing reliably is a warning: compilation alone is not validity.

**Limitations / gaps.** Improves existing tests rather than generating from an API
specification; industrial/proprietary codebase; human review still required.

## 3. RESTGPT — LLMs for REST API testing

**Citation:** M. Kim, T. Stennett, D. Shah, S. Sinha, A. Orso, "Leveraging Large Language Models
to Improve REST API Testing," *46th IEEE/ACM International Conference on Software Engineering,
New Ideas and Emerging Results (ICSE-NIER '24)*, 2024. DOI: 10.1145/3639476.3639769.
arXiv:2312.00894.

**Approach.** RESTGPT takes an API specification, uses an LLM to extract machine-interpretable
rules from natural-language descriptions, and generates example parameter values. The original
OpenAPI specification is augmented with these rules and values so that existing REST testing
tools can use them. The evaluation compares rule extraction and value generation with prior
description-mining techniques.

**Findings.**
- RESTGPT outperformed existing techniques at extracting rules and generating parameter values
  from natural-language descriptions.
- The authors argue that structured specification fields alone miss information in free-text
  descriptions, which limits test coverage.

**Relevance to this project.**
- Closest work to the project's OpenAPI-driven setting: the specification is the primary input.
- Supports using the LLM for *analysis and data generation*, while deterministic code executes tests.
- Highlights specification quality as a threat to validity; our analyzer must handle missing
  descriptions and schemas conservatively.

**Limitations / gaps.** Enhances specifications rather than producing a complete, reviewed,
executable test suite; LLM-extracted rules can be wrong; no comparison of manual vs AI suites
using coverage and mutation score.

## 4. JUnit test generation with LLMs — empirical study

**Citation:** M. L. Siddiq, J. C. S. Santos, R. H. Tanvir, N. Ulfat, F. A. Rifat, V. C. Lopes,
"Using Large Language Models to Generate JUnit Tests: An Empirical Study," *28th International
Conference on Evaluation and Assessment in Software Engineering (EASE '24)*, 2024.
DOI: 10.1145/3661167.3661216. arXiv:2305.00418.

**Approach.** Three code models (Codex, GPT-3.5-Turbo, StarCoder) generate JUnit tests for Java.
The study varies the context given in the prompt and evaluates compilation rate, correctness,
line/branch coverage, and test smells on HumanEval and the EvoSuite SF110 benchmark.

**Findings.**
- On HumanEval, line coverage ranged from 67% to 87.7% and branch coverage from 69.3% to
  92.8% (Codex 4K highest); LLM coverage was still below manual tests and EvoSuite on that set.
- On SF110, no model exceeded 2% coverage.
- Generated tests contained smells such as duplicated asserts and empty tests; heuristic fixes
  improved compilation rates.

**Relevance to this project.**
- Directly relevant language (Java/JUnit) and metrics (JaCoCo line/branch coverage).
- Strong caution that results on small exercises do not transfer to real projects; our sample API
  and baseline must be realistic enough to expose this.
- Motivates measuring validity (compiles/executes) separately from coverage and checking for
  trivial or duplicated tests.

**Limitations / gaps.** Unit tests for isolated methods, not HTTP-level API tests; older models;
prompt/context choices differ from our structured-output approach.

## 5. Survey — software testing with LLMs

**Citation:** J. Wang, Y. Huang, C. Chen, Z. Liu, S. Wang, Q. Wang, "Software Testing with Large
Language Models: Survey, Landscape, and Vision," *IEEE Transactions on Software Engineering*,
vol. 50, no. 4, pp. 911–936, 2024. DOI: 10.1109/TSE.2024.3368208. arXiv:2307.07221.

**Approach.** Reviews 102 studies that apply LLMs to software testing. Classifies testing tasks,
models, prompt-engineering techniques, and accompanying techniques, then lists challenges and
opportunities.

**Findings.**
- Test-case preparation and program repair are the most representative tasks.
- The field uses a wide range of models and prompts; results are difficult to compare across studies.
- Open challenges include the oracle problem, evaluation quality, data leakage, and non-determinism.

**Relevance to this project.**
- Provides the taxonomy and justifies the project's evaluation design: baseline comparison,
  repeated runs, validity rate, coverage, mutation score, and honest reporting.
- Confirms that an end-to-end, reproducible pipeline with recorded prompts/models is a useful contribution.

**Limitations / gaps.** Literature cut-off is early 2023 and the area moves quickly; the survey
does not evaluate a specific pipeline.

## Synthesis and gap addressed by this project

Across the five papers:

1. LLMs can produce useful tests, but raw output is unreliable: build/pass/coverage filters and
   human review are consistently needed.
2. Context matters. Specification text, schemas, examples, and error feedback improve results.
3. Validity and fault detection must be measured separately from coverage; high coverage does
   not imply effective tests.
4. REST-specific work (RESTGPT) enriches specifications but does not deliver a full
   spec → generate → validate → review → execute → evaluate pipeline with a manual baseline.

This project addresses that gap for a Java REST API: OpenAPI input, structured JSON definitions
(no generated Java code), deterministic validation/execution, and a controlled comparison of
manual vs Gemini-generated suites using JaCoCo, PIT, injected defects, validity, and time.

## References (for the dissertation bibliography)

1. Schäfer et al., IEEE TSE 50(1), 2024, DOI 10.1109/TSE.2023.3334955.
2. Alshahwan et al., FSE 2024 Industry Papers, arXiv:2402.09171.
3. Kim et al., ICSE-NIER 2024, DOI 10.1145/3639476.3639769.
4. Siddiq et al., EASE 2024, DOI 10.1145/3661167.3661216.
5. Wang et al., IEEE TSE 50(4), 2024, DOI 10.1109/TSE.2024.3368208.
