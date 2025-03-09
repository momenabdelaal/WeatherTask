# Development Guide

## Branch Strategy

### Main Branches
1. `master`
   - Production-ready code
   - Protected branch
   - Requires PR approval and passing CI checks
   - Auto-generates release APK
   - Tagged for releases
   - No direct commits allowed

2. `development`
   - Integration branch for features
   - Protected branch
   - Requires passing CI checks
   - Auto-generates debug APK
   - Merges into master via PR

### Feature Development
1. Create feature branch from `development`:
   ```bash
   git checkout development
   git pull origin development
   git checkout -b feature/your-feature-name
   ```

2. Keep your branch updated:
   ```bash
   git checkout development
   git pull origin development
   git checkout feature/your-feature-name
   git rebase development
   ```

3. Commit guidelines:
   - Use descriptive commit messages
   - Start with verb (add, fix, update)
   - Reference issue number if applicable
   ```bash
   git commit -m "feat: add weather icon caching (#123)"
   ```

## Code Quality

### Before Committing
1. Run code style checks:
   ```bash
   ./gradlew ktlintCheck
   ./gradlew lint
   ```

2. Run tests:
   ```bash
   ./gradlew test
   ```

3. Fix any issues:
   ```bash
   ./gradlew ktlintFormat
   ```

### Pull Request Process
1. Update your branch:
   ```bash
   git checkout development
   git pull origin development
   git checkout your-feature-branch
   git rebase development
   ```

2. Push your changes:
   ```bash
   git push origin your-feature-branch
   ```

3. Create PR to `development`
   - Use PR template
   - Add screenshots if UI changes
   - Reference related issues

4. Address review comments
   - Make requested changes
   - Push updates
   - Request re-review

### Release Process
1. Create release branch:
   ```bash
   git checkout development
   git pull origin development
   git checkout -b release/v1.x.x
   ```

2. Version bump:
   - Update version in app/build.gradle.kts
   - Update CHANGELOG.md

3. Create PR to `master`
   - Comprehensive testing
   - Documentation updates
   - Version verification

4. After merge:
   - Tag release
   - Update development
   ```bash
   git tag -a v1.x.x -m "Release v1.x.x"
   git push origin v1.x.x
   ```

## Testing Strategy

### Unit Tests
- Required for:
  * ViewModels
  * Use Cases
  * Repositories
  * Utilities

### UI Tests
- Required for:
  * Critical user flows
  * Complex UI components
  * Navigation

### Test Coverage
- Minimum 80% coverage required
- Focus on business logic
- Mock external dependencies

## Error Handling
- Use centralized error handler
- Arabic error messages
- Proper error states in UI
- Meaningful error reporting

## Documentation
- Update README.md
- Code documentation
- Architecture decisions
- API documentation

## Security
- No API keys in code
- Use local.properties
- Proper Proguard rules
- Secure data storage
