# App Store Review & Rating Service - Development Instructions

## Project Overview

This is a Spring Boot application implementing a scalable Review & Rating service with:
- **DynamoDB** for data persistence (via LocalStack)
- **Elasticsearch/OpenSearch** for search functionality
- **In-memory aggregator** for real-time rating calculations
- **Dual-write strategy** for data consistency

## Critical Instructions for Development

### 1. **ALWAYS READ THE IMPLEMENTATION PLAN FIRST**
Before making any changes, **MUST READ** `IMPLEMENTATION_PLAN.md` to understand:
- Current progress and completed tasks
- Architecture decisions and design patterns
- Next steps and dependencies
- Overall project structure

### 2. **Development Approach - Phase by Phase**

**NEVER implement everything at once. Follow this strict process:**

1. **Read Implementation Plan** - Understand current state
2. **Identify Next Phase** - Pick 1-2 related tasks only
3. **Implement Phase** - Write code for selected tasks
4. **Ask for Review** - Present changes and get user approval
5. **Get Acknowledgment** - Wait for user confirmation before proceeding
6. **Repeat** - Move to next phase only after approval

**Example Phases:**
- Phase 1: Configuration classes (DynamoDBConfig, ElasticsearchConfig)
- Phase 2: Domain entities and DTOs
- Phase 3: Repository layer
- Phase 4: Core services (one at a time)
- Phase 5: Controllers (one at a time)
- Phase 6: Exception handling and monitoring

### 3. **Code Quality Standards**

#### **Design Patterns - Use Wisely**
- **Observer Pattern**: Use for async operations (rating updates → aggregator, comment creation → ES sync)
- **Strategy Pattern**: For multiple scoring algorithms in aggregator
- **Repository Pattern**: For data access abstraction
- **Service Layer Pattern**: For business logic separation

#### **SOLID Principles**
- **Single Responsibility**: Each class has one clear purpose
- **Open/Closed**: Extend functionality without modifying existing code
- **Liskov Substitution**: Interfaces should be substitutable
- **Interface Segregation**: Small, focused interfaces
- **Dependency Inversion**: Depend on abstractions, not concretions

#### **What NOT to Do**
- ❌ Don't wrap trivial SDK calls in 3 layers of abstraction
- ❌ Don't create interfaces for single implementations
- ❌ Don't force observer pattern for simple method calls
- ❌ Don't create factories for simple POJOs
- ❌ Don't over-engineer simple CRUD operations
- ❌ Don't overwrite existing code without explicit permission

#### **What TO Do**
- ✅ Keep class responsibilities clean and focused
- ✅ Use consistent naming conventions
- ✅ Add short design notes for complex patterns
- ✅ Use events only when async operations make sense
- ✅ Keep interfaces only where multiple implementations exist
- ✅ Write self-documenting code with clear method names

### 4. **Architecture Guidelines**

#### **Service Layer Structure**
```
Controller → Service → Repository → Database
     ↓
   Events (Observer) → Async Operations
```

#### **Event-Driven Components**
- **Rating Events**: When rating is created/updated → trigger aggregator update
- **Comment Events**: When comment is persisted → trigger ES indexing
- **App Events**: When app is created → trigger ES indexing

#### **Error Handling Strategy**
- Use global exception handler for consistent responses
- Log errors but don't fail the main operation for ES sync failures

### 5. **Implementation Checklist**

Before implementing any phase, ensure:

- [ ] Read `IMPLEMENTATION_PLAN.md` completely
- [ ] Understand current progress and next steps
- [ ] Identify dependencies and prerequisites
- [ ] Plan the specific phase (1-2 tasks max)
- [ ] Consider design patterns and SOLID principles
- [ ] Write clean, self-documenting code
- [ ] Add appropriate logging and error handling
- [ ] Test the implementation locally

### 6. **Code Review Criteria**

When presenting code for review, include:

1. **Design Decisions**: Why this approach was chosen
2. **Pattern Usage**: Which design patterns were used and why
3. **SOLID Compliance**: How the code follows SOLID principles
4. **Error Handling**: How errors are managed
5. **Testing Strategy**: How the code can be tested
6. **Performance Considerations**: Any performance implications

### 7. **File Organization**

Follow this package structure:
```
com.example.appstore/
├── config/          # Configuration classes
├── domain/          # Entity classes
├── dto/            # Request/Response DTOs
├── repository/     # Data access layer
├── service/        # Business logic
├── controller/     # REST endpoints
├── event/          # Event classes
├── exception/      # Custom exceptions
└── util/           # Utility classes
```

### 8. **Testing Requirements**

- Write unit tests for business logic
- Mock external dependencies (DynamoDB, ES)
- Test error scenarios and edge cases
- Use Testcontainers for integration tests
- Test aggregator recovery scenarios

### 9. **Documentation Standards**

Add design notes for complex implementations:
```java
/**
 * RatingAggregatorService uses Strategy pattern to support multiple scoring algorithms.
 * Observer pattern is used to receive rating change events asynchronously.
 */
@Service
public class RatingAggregatorService {
    // Implementation
}
```

### 10. **Common Pitfalls to Avoid**

- **Over-abstraction**: Don't create unnecessary layers
- **Premature optimization**: Focus on correctness first
- **Tight coupling**: Use dependency injection properly
- **Missing error handling**: Always handle failure scenarios
- **Inconsistent naming**: Follow Java conventions
- **Large methods**: Keep methods focused and small
- **Missing validation**: Validate all inputs
- **Poor logging**: Add meaningful log messages

### 11. **Development Workflow**

1. **Start**: Read `IMPLEMENTATION_PLAN.md`
2. **Plan**: Identify next phase (1-2 tasks)
3. **Code**: Implement following quality standards
4. **Review**: Present changes with explanations
5. **Iterate**: Make changes based on feedback
6. **Approve**: Get user acknowledgment
7. **Continue**: Move to next phase

### 12. **Key Technologies**

- **Spring Boot 3.5.6** with Java 21
- **AWS SDK v2** for DynamoDB operations
- **OpenSearch Java Client** for search operations
- **LocalStack** for local DynamoDB
- **Docker Compose** for infrastructure
- **Lombok** for reducing boilerplate

### 13. **Success Criteria**

A successful implementation should:
- ✅ Follow the architecture defined in HLD.md and LLD.md
- ✅ Implement all required APIs with proper error handling
- ✅ Use appropriate design patterns without over-engineering
- ✅ Maintain data consistency between DynamoDB and ES
- ✅ Handle aggregator recovery on service restart
- ✅ Provide comprehensive logging and monitoring
- ✅ Be testable and maintainable

---

## Remember: Quality over Speed

Take time to implement each phase correctly. It's better to have well-designed, maintainable code that follows best practices than to rush through implementation and create technical debt.

**Always ask for review and acknowledgment before proceeding to the next phase.**
