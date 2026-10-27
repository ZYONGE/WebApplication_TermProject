<div align="center">

# 웹프로그래밍응용_기말프로젝트_쇼핑몰

</div>

#### Languages

<img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=openjdk&logoColor=white"/>
<img src="https://img.shields.io/badge/JSP-E76F00?style=for-the-badge&logo=java&logoColor=white"/>
<img src="https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white"/>
<img src="https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white"/>
<img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black"/>
<img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/>

#### Tools

<img src="https://img.shields.io/badge/Eclipse-2C2255?style=for-the-badge&logo=eclipseide&logoColor=white"/>
<img src="https://img.shields.io/badge/VS_Code-007ACC?style=for-the-badge&logo=visualstudiocode&logoColor=white"/>
<img src="https://img.shields.io/badge/DBeaver-382923?style=for-the-badge&logo=dbeaver&logoColor=white"/>
<img src="https://img.shields.io/badge/HeidiSQL-4D9DE0?style=for-the-badge&logoColor=white"/>
<img src="https://img.shields.io/badge/Dreamweaver-470137?style=for-the-badge&logoColor=white"/>
<img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white"/>
<img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white"/>

#### Platform

<img src="https://img.shields.io/badge/macOS-000000?style=for-the-badge&logo=apple&logoColor=white"/>
<img src="https://img.shields.io/badge/Windows-0078D4?style=for-the-badge&logo=windows&logoColor=white"/>

#### Server

<img src="https://img.shields.io/badge/Tomcat-F8DC75?style=for-the-badge&logo=apachetomcat&logoColor=black"/>

#### AI Agent

<img src="https://img.shields.io/badge/Claude_Code-D97757?style=for-the-badge&logo=anthropic&logoColor=white"/>

---
## Table of Contents

- [Project Overview](#project-overview)
- [아키텍처 명세](#아키텍처-명세architecture)
- [DB 설계 명세](#db-설계-명세database)
  - [ERD](#erd)
  - [테이블 명세](#테이블-명세tables)
  - [DDL](#ddl)
- [기능 구현 명세](#기능-구현-명세features)
- [핵심 구현 명세](#핵심-구현-명세implementation)
- [URL 매핑 명세](#url-매핑-명세url-mapping)
- [UI](#ui)
- [Project Structure](#project-structure)
- [Build & Run](#build--run)
  - [Prerequisites](#prerequisites)
  - [DB 초기화](#db-초기화)
  - [Eclipse 설정](#eclipse-설정)
  - [Run](#run)
  - [Deploy (WAR)](#deploy-war)
- [License](#license)
- [Author](#author)
- [Motivation](#motivation)
---

## Project Overview

- **아키텍처**:
- **DB 연동**:
- **인증 방식**:

---

## 아키텍처 명세(Architecture)

| 계층 | 구성 요소 | 역할 |
|---|---|---|
| Controller | | |
| Model | | |
| View | | |
| Filter | | |

```
<!-- 요청 처리 흐름 다이어그램 -->
```

---

## DB 설계 명세(Database)

### ERD

```
<!-- ERD 다이어그램 또는 이미지 (docs/erd.png) -->
```

### 테이블 명세(Tables)

| 테이블 | 설명 | 주요 컬럼 |
|---|---|---|
| | | |

### DDL

```sql
-- 테이블 생성 스크립트 (sql/schema.sql)
```

---

## 기능 구현 명세(Features)

### 기능명

- 

---

## 핵심 구현 명세(Implementation)

### 구현 항목명

<!-- 설명 -->

```java
// 핵심 코드
```

---

## URL 매핑 명세(URL Mapping)

| Method | URL | Servlet | View |
|---|---|---|---|
| | | | |

---

## UI

```
<!-- 화면 레이아웃 또는 스크린샷 -->
```

---

## Project Structure

```
WebApplication_TermProject/
├── src/
│   └── main/
│       └── java/                    # Java 소스 (Servlet, DAO, DTO 등)
├── WebContents/                     # 웹 루트 (JSP, HTML, CSS, JS, 이미지)
│   ├── META-INF/
│   │   └── MANIFEST.MF
│   └── WEB-INF/
│       ├── lib/                     # 외부 라이브러리 (WAR 배포 시 포함)
│       │   ├── mysql-connector-j-9.7.0.jar
│       │   ├── commons-dbcp-1.4.jar
│       │   ├── commons-pool-1.5.7.jar
│       │   ├── taglibs-standard-impl-1.2.5.jar
│       │   ├── taglibs-standard-spec-1.2.5.jar
│       │   └── cos.jar
│       └── web.xml                  # 배포 서술자 (Servlet 4.0)
├── build/                           # 컴파일 결과물 (git 제외)
├── .settings/                       # Eclipse 프로젝트 설정 (Facet, Context Root)
├── .classpath                       # Eclipse 빌드 경로 (JavaSE-17, Tomcat v9.0)
├── .project                         # Eclipse 프로젝트 정의
├── LICENSE
├── .gitignore
└── README.md
```

---

## Build & Run

### Prerequisites

| 항목 | 버전 |
|---|---|
| JDK | OpenJDK 17.0.19|
| Apache Tomcat | 9.0.117 (Servlet 4.0 / JSP 2.3) |
| Dynamic Web Module | 4.0 |
| MySQL | 9.6.0|
| MySQL Connector/J | 9.7.0 |
| IDE | Eclipse IDE for Enterprise Java and Web Developers 4.39 (2026-03) |
| Editor | Visual Studio Code|
| DB Client | DBeaver, Heidisql |

### DB 초기화

```sql
-- DB 사용자 생성 / 스키마 적용
```

### Eclipse 설정

1. 

### Run

1. 

```
http://localhost:8080/
```

### Deploy (WAR)

1. 

---

## License

This project is licensed under the MIT License.

See the [LICENSE](LICENSE) file for details.

---

## Author

Name: Jiyong Kim, Hawon Yoon, Minjun Kim

Profile: https://github.com/ZYONGE, https://github.com/yhw2528-cmd, https://github.com/minjun2388

## Motivation

For A+ !!!!
