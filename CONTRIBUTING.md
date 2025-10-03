# Contributing to DisasterMesh

Thank you for your interest in contributing to DisasterMesh! This project aims to provide reliable emergency communication tools, and we welcome contributions from developers who share this mission.

## 🤝 How to Contribute

### Reporting Issues
- **Search existing issues** before creating a new one
- **Use clear, descriptive titles** for bug reports and feature requests
- **Include steps to reproduce** for bug reports
- **Provide device/OS information** when relevant
- **Add screenshots or logs** when helpful

### Suggesting Features
- **Check if the feature already exists** or is planned
- **Explain the use case** and why it would be valuable
- **Consider emergency scenarios** and how the feature helps
- **Think about offline functionality** and resource constraints

### Code Contributions

#### Getting Started
1. **Fork the repository** on GitHub
2. **Clone your fork** locally
3. **Create a feature branch** from `main`
4. **Set up the development environment** (see README.md)

#### Development Guidelines

##### Code Style
- **Follow Kotlin conventions** and Android best practices
- **Use meaningful names** for variables, functions, and classes
- **Add KDoc comments** for public APIs
- **Keep functions small** and focused on single responsibilities
- **Use consistent indentation** (4 spaces)

##### Architecture
- **Follow MVVM pattern** where applicable
- **Use Android Architecture Components** (ViewModel, LiveData, etc.)
- **Separate concerns** between UI, business logic, and data layers
- **Handle configuration changes** properly
- **Consider battery optimization** and performance

##### Emergency-Focused Development
- **Prioritize reliability** over fancy features
- **Design for stress situations** - simple, clear interfaces
- **Test offline scenarios** thoroughly
- **Consider accessibility** for users with disabilities
- **Optimize for low-battery situations**

##### Security Considerations
- **Never commit sensitive data** (API keys, passwords, certificates)
- **Use secure communication** protocols
- **Validate all user inputs**
- **Follow Android security best practices**
- **Consider privacy implications** of location data

#### Testing
- **Write unit tests** for new functionality
- **Test on multiple devices** and Android versions
- **Test offline scenarios** and edge cases
- **Verify accessibility** with TalkBack and other tools
- **Test in low-battery conditions**

#### Pull Request Process

1. **Update documentation** if needed
2. **Add tests** for new features
3. **Ensure all tests pass**
4. **Update CHANGELOG.md** with your changes
5. **Create a clear PR description** explaining the changes
6. **Link related issues** in the PR description

##### PR Description Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] Tested on multiple devices
- [ ] Tested offline scenarios

## Emergency Use Considerations
- [ ] Works reliably under stress
- [ ] Maintains performance in low-battery situations
- [ ] Accessible to users with disabilities
- [ ] Clear and intuitive for emergency use

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No sensitive data committed
```

## 🏗️ Development Setup

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or later
- Android SDK 24+ (API level 24)
- Git

### Local Development
```bash
# Clone your fork
git clone https://github.com/yourusername/DisasterMesh.git
cd DisasterMesh

# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and commit
git add .
git commit -m "Add your feature"

# Push to your fork
git push origin feature/your-feature-name
```

### Building
```bash
# Debug build
./gradlew assembleDebug

# Run tests
./gradlew test

# Run lint checks
./gradlew lint
```

## 📋 Priority Areas for Contribution

### High Priority
- **Battery optimization** improvements
- **Accessibility** enhancements
- **Offline functionality** improvements
- **Network reliability** enhancements
- **Security** improvements

### Medium Priority
- **UI/UX** improvements for emergency scenarios
- **Performance** optimizations
- **Documentation** improvements
- **Test coverage** expansion
- **Internationalization** (i18n)

### Low Priority
- **New features** (discuss first)
- **Code refactoring** (ensure no functionality loss)
- **Developer tools** and utilities

## 🚨 Emergency Use Guidelines

When contributing to DisasterMesh, always consider:

### Reliability First
- **Fail gracefully** - never crash in emergency situations
- **Provide fallbacks** for all critical functionality
- **Test edge cases** thoroughly
- **Handle network failures** elegantly

### Simplicity in Crisis
- **Keep interfaces simple** and intuitive
- **Use clear, large buttons** for critical actions
- **Provide immediate feedback** for user actions
- **Minimize cognitive load** during stressful situations

### Accessibility
- **Support screen readers** and other assistive technologies
- **Ensure good color contrast** for visibility
- **Provide alternative input methods**
- **Test with accessibility tools**

### Performance
- **Optimize for older devices** that may be more common in disaster areas
- **Minimize battery usage** when possible
- **Handle low-memory situations** gracefully
- **Keep app size reasonable** for limited storage

## 📞 Communication

### Getting Help
- **GitHub Discussions** for general questions
- **GitHub Issues** for bugs and feature requests
- **Code reviews** for technical discussions

### Community Guidelines
- **Be respectful** and inclusive
- **Focus on the mission** of emergency preparedness
- **Help newcomers** get started
- **Share knowledge** and best practices
- **Consider diverse perspectives** and use cases

## 🏆 Recognition

Contributors will be recognized in:
- **README.md** contributors section
- **Release notes** for significant contributions
- **GitHub contributors** page

## 📄 Legal

By contributing to DisasterMesh, you agree that:
- Your contributions will be licensed under the MIT License
- You have the right to submit the contributions
- You understand this is emergency software with specific reliability requirements

---

**Thank you for helping make emergency communication more reliable and accessible!** 🚨❤️