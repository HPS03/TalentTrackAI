package com.talenttrack.config;

import com.talenttrack.ai.AiModels;
import com.talenttrack.ai.LocalMatcher;
import com.talenttrack.entity.*;
import com.talenttrack.repository.*;
import com.talenttrack.service.Skills;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Creates the admin account and (optionally) demo recruiters, candidates, jobs and a populated board. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email}")
    private String adminEmail;
    @Value("${app.seed.admin-password}")
    private String adminPassword;
    @Value("${app.seed.demo-data}")
    private boolean demoData;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
            userRepository.save(user("Platform Admin", adminEmail, adminPassword, Role.ADMIN, null));
            log.info("Seeded admin account {}", adminEmail);
        }
        if (demoData && userRepository.countByRole(Role.RECRUITER) == 0) {
            seedDemo();
            log.info("Seeded demo data (recruiter@talenttrack.ai / Recruiter@123, candidate@talenttrack.ai / Candidate@123)");
        }
    }

    private void seedDemo() {
        User recruiter = userRepository.save(user("Priya Sharma", "recruiter@talenttrack.ai", "Recruiter@123",
                Role.RECRUITER, "NimbusTech"));
        User recruiter2 = userRepository.save(user("Arjun Mehta", "hr@cloudkart.io", "Recruiter@123",
                Role.RECRUITER, "CloudKart"));

        Job backend = job(recruiter, "Java Backend Developer", "Bengaluru", JobType.FULL_TIME, WorkMode.HYBRID, 1,
                800000, 1400000, "Java, Spring Boot, REST APIs, MySQL, Docker, Microservices, JUnit",
                "We are looking for a Java Backend Developer to design and build scalable REST APIs with Spring Boot. "
                        + "You will own microservices end to end, write clean, tested code with JUnit, model data in MySQL "
                        + "and ship with Docker and CI/CD pipelines. Experience with Spring Security and JWT is a plus.");
        Job intern = job(recruiter, "Full Stack Developer Intern", "Remote", JobType.INTERNSHIP, WorkMode.REMOTE, 0,
                25000, 40000, "React, JavaScript, Java, Spring Boot, Git, HTML, CSS",
                "Six-month paid internship for students who love building products. You will build React components, "
                        + "integrate them with Spring Boot REST APIs, write unit tests and learn Git-based collaboration "
                        + "with senior engineers. Strong JavaScript fundamentals required.");
        job(recruiter, "Data Analyst Intern", "Pune", JobType.INTERNSHIP, WorkMode.ONSITE, 0, 20000, 30000,
                "Python, SQL, Excel, Power BI, Statistics, Pandas",
                "Join our analytics team to clean and analyse product data using Python, Pandas and SQL. You will build "
                        + "Power BI dashboards and present insights to stakeholders. Knowledge of statistics is expected.");
        job(recruiter2, "React Frontend Engineer", "Hyderabad", JobType.FULL_TIME, WorkMode.HYBRID, 2, 1000000,
                1800000, "React, TypeScript, Redux, Tailwind CSS, Jest, REST APIs",
                "Build delightful, accessible user interfaces with React and TypeScript. You will manage state with Redux, "
                        + "style with Tailwind CSS, test with Jest and collaborate closely with designers and backend teams.");
        job(recruiter2, "DevOps Engineer", "Remote", JobType.FULL_TIME, WorkMode.REMOTE, 3, 1500000, 2500000,
                "Docker, Kubernetes, AWS, Terraform, Jenkins, Linux, CI/CD",
                "Own our cloud infrastructure on AWS. Automate everything with Terraform, run workloads on Kubernetes, "
                        + "and maintain CI/CD pipelines in Jenkins. Strong Linux and scripting skills required.");
        job(recruiter2, "Machine Learning Intern", "Bengaluru", JobType.INTERNSHIP, WorkMode.HYBRID, 0, 30000,
                50000, "Python, Machine Learning, Scikit-learn, NLP, Pandas, NumPy",
                "Work on NLP models that power resume screening. Train and evaluate models with scikit-learn, process "
                        + "text data with Pandas and NumPy, and help deploy models behind FastAPI services.");

        User candidate = candidate("Rahul Verma", "candidate@talenttrack.ai",
                "Java Developer | Spring Boot | React", 1,
                "Java, Spring Boot, MySQL, REST APIs, React, Git, Docker, JavaScript");
        User c2 = candidate("Sneha Iyer", "sneha@example.com", "Backend Engineer", 2,
                "Java, Spring Boot, Microservices, MySQL, Docker, Kafka, JUnit");
        User c3 = candidate("Aman Gupta", "aman@example.com", "CS Undergrad", 0, "Java, HTML, CSS, JavaScript, Git");
        User c4 = candidate("Neha Kapoor", "neha@example.com", "Full Stack Developer", 1,
                "React, Java, Spring Boot, REST APIs, JUnit, Git");
        User c5 = candidate("Vikram Rao", "vikram@example.com", "Software Engineer", 3,
                "Java, Spring Boot, Docker, Kubernetes, AWS, MySQL");

        apply(candidate, backend, ApplicationStage.INTERVIEW, 0);
        apply(c2, backend, ApplicationStage.OFFER, 0);
        apply(c3, backend, ApplicationStage.APPLIED, 0);
        apply(c4, backend, ApplicationStage.SCREENING, 0);
        apply(c5, backend, ApplicationStage.SCREENING, 1);
        apply(c3, intern, ApplicationStage.APPLIED, 0);
        apply(c4, intern, ApplicationStage.INTERVIEW, 0);
    }

    private User user(String name, String email, String password, Role role, String company) {
        return User.builder().fullName(name).email(email).password(passwordEncoder.encode(password))
                .role(role).companyName(company).enabled(true).build();
    }

    private User candidate(String name, String email, String headline, int years, String skills) {
        User u = userRepository.save(user(name, email, "Candidate@123", Role.CANDIDATE, null));
        profileRepository.save(CandidateProfile.builder().user(u).headline(headline).experienceYears(years)
                .skills(skills.replace(", ", ",")).location("India")
                .bio(headline + " passionate about building reliable software. Skilled in " + skills + ".")
                .education("B.Tech in Computer Science").build());
        return u;
    }

    private Job job(User recruiter, String title, String location, JobType type, WorkMode mode, int minExp,
                    int salMin, int salMax, String skills, String description) {
        return jobRepository.save(Job.builder().recruiter(recruiter).title(title)
                .companyName(recruiter.getCompanyName()).location(location).jobType(type).workMode(mode)
                .minExperience(minExp).salaryMin(salMin).salaryMax(salMax).skills(skills.replace(", ", ","))
                .description(description).openings(2).status(JobStatus.OPEN)
                .deadline(LocalDate.now().plusMonths(2)).build());
    }

    private void apply(User candidate, Job job, ApplicationStage stage, int position) {
        CandidateProfile p = profileRepository.findByUserId(candidate.getId()).orElseThrow();
        AiModels.MatchResult m = LocalMatcher.match(new AiModels.MatchRequest(p.getBio(), Skills.split(p.getSkills()),
                p.getExperienceYears(), job.getDescription(), Skills.split(job.getSkills()), job.getMinExperience()));
        Application a = applicationRepository.save(Application.builder().job(job).candidate(candidate).stage(stage)
                .atsScore(m.overall()).skillScore(m.skillScore()).semanticScore(m.semanticScore())
                .experienceScore(m.experienceScore()).matchedSkills(Skills.join(m.matchedSkills()))
                .missingSkills(Skills.join(m.missingSkills())).aiSummary(m.summary()).boardPosition(position)
                .build());
        activityRepository.save(ApplicationActivity.builder().application(a).actor(candidate)
                .type(ActivityType.APPLIED).toStage(ApplicationStage.APPLIED).message("Applied").build());
        if (stage != ApplicationStage.APPLIED) {
            activityRepository.save(ApplicationActivity.builder().application(a).actor(job.getRecruiter())
                    .type(ActivityType.STAGE_CHANGED).fromStage(ApplicationStage.APPLIED).toStage(stage).build());
        }
    }
}
