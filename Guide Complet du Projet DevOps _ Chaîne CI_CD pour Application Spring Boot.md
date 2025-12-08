# Guide Complet du Projet DevOps : Chaîne CI/CD pour Application Spring Boot

Ce document sert de guide détaillé pour la réalisation de l'exercice pratique DevOps, couvrant l'Intégration Continue (CI), la Livraison Continue (CD), la qualité de code, la gestion des artéfacts et la supervision.

## 1. Objectifs du Projet

L'objectif principal est de mettre en place une chaîne CI/CD automatisée pour une application de gestion des événements développée avec Spring Boot, en utilisant les outils suivants :

| Catégorie | Outil | Rôle dans le Projet |
| :--- | :--- | :--- |
| **Gestion de Code** | Git / GitHub | Dépôt du code source et déclencheur du pipeline. |
| **Orchestration CI/CD** | Jenkins | Moteur d'automatisation du pipeline. |
| **Qualité de Code** | SonarQube | Analyse statique du code et application de la *Quality Gate*. |
| **Gestion des Artéfacts** | Nexus Repository | Stockage des livrables (JAR/WAR) et des dépendances. |
| **Conteneurisation** | Docker | Création d'images conteneurisées pour l'application et les services. |
| **Orchestration Locale** | Docker Compose | Déploiement local de l'application et de la base de données MySQL. |
| **Supervision** | Prometheus / Grafana | Collecte des métriques et visualisation de l'état des conteneurs. |

## 2. Préparation Initiale et Récupération du Code

### 2.1. Récupération du Code Source

Étant donné que le code source est inaccessible (lien Google Drive), nous allons **assumer** une structure de projet Spring Boot/Maven standard.

1.  **Cloner le dépôt Git :**
    ```bash
    git clone [URL_DE_VOTRE_DEPOT_GITHUB]
    cd [NOM_DU_PROJET]
    ```
2.  **Ajouter l'enseignant au dépôt :** Conformément à la remarque 1, assurez-vous d'ajouter l'enseignant comme collaborateur sur GitHub.

### 2.2. Implémentation des Tests Unitaires (Tâche 2)

L'énoncé demande d'implémenter des tests unitaires en utilisant Mock. Pour un projet Spring Boot/Maven, cela implique généralement l'utilisation de **JUnit 5** et de **Mockito**.

**Action :** S'assurer que le projet contient des classes de test (e.g., `EventServiceTest.java`) utilisant `@Mock` ou `@MockBean` pour simuler les dépendances (comme le `Repository`) et tester la logique métier de l'application.

## 3. Mise en Place de l'Infrastructure DevOps (Tâche 1 & 6 - Partie Infra)

L'architecture requise implique plusieurs outils. La méthode la plus efficace pour les mettre en place est d'utiliser **Docker Compose** pour créer un environnement de laboratoire isolé.

Nous aurons besoin de conteneurs pour : **Jenkins**, **SonarQube**, **Nexus**, **Prometheus**, et **Grafana**.

**Fichier : `docker-compose-infra.yml`**

```yaml
version: '3.8'
services:
  # 1. Jenkins
  jenkins:
    image: jenkins/jenkins:lts
    privileged: true
    user: root
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock # Permet à Jenkins d'utiliser Docker
    environment:
      - JAVA_OPTS=-Djenkins.install.runSetupWizard=true
    networks:
      - devops-net

  # 2. SonarQube
  sonarqube:
    image: sonarqube:latest
    ports:
      - "9000:9000"
    environment:
      - SONAR_JDBC_URL=jdbc:postgresql://sonardb:5432/sonar
      - SONAR_JDBC_USERNAME=sonar
      - SONAR_JDBC_PASSWORD=sonar
    networks:
      - devops-net

  # 3. Nexus Repository
  nexus:
    image: sonatype/nexus3
    ports:
      - "8081:8081"
    volumes:
      - nexus_data:/nexus-data
    networks:
      - devops-net

  # 4. Prometheus
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command: --config.file=/etc/prometheus/prometheus.yml
    networks:
      - devops-net

  # 5. Grafana
  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    networks:
      - devops-net

volumes:
  jenkins_home:
  nexus_data:

networks:
  devops-net:
    driver: bridge
```

**Action :** Créer le fichier `prometheus.yml` pour la configuration de Prometheus.

### 3.1. Configuration de Prometheus

**Fichier : `prometheus.yml`**

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080'] # Sera ajouté plus tard pour la supervision des conteneurs
```

**Action :** Lancer l'infrastructure : `docker-compose -f docker-compose-infra.yml up -d`

## 4. Conteneurisation de l'Application (Tâche 3 - Partie Docker)

### 4.1. Dockerfile de l'Application

Le `Dockerfile` permet de créer l'image conteneur de l'application Spring Boot (livrable JAR).

**Fichier : `Dockerfile`** (à placer à la racine du projet Spring Boot)

```dockerfile
# Phase 1: Build stage (Utilisation de Maven pour compiler)
FROM maven:3.8.6-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Phase 2: Run stage (Image légère pour l'exécution)
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copie du JAR depuis la phase de build
COPY --from=build /app/target/*.jar app.jar
# Exposition du port par défaut de Spring Boot
EXPOSE 8080
# Point d'entrée pour lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.2. Docker Compose pour le Déploiement Local (Tâche 3 - Partie Lancement)

Ce fichier permet de lancer l'application conteneurisée et la base de données MySQL simultanément.

**Fichier : `docker-compose.yml`** (à placer à la racine du projet Spring Boot)

```yaml
version: '3.8'
services:
  # 1. Application Spring Boot
  event-app:
    build: . # Utilise le Dockerfile dans le répertoire courant
    image: VOTRE_DOCKERHUB_USER/event-app:latest # Image qui sera poussée
    ports:
      - "8080:8080"
    environment:
      # Configuration de la base de données pour Spring Boot
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql-db:3306/eventdb
      - SPRING_DATASOURCE_USERNAME=user
      - SPRING_DATASOURCE_PASSWORD=password
    depends_on:
      - mysql-db
    networks:
      - app-net

  # 2. Base de Données MySQL
  mysql-db:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=rootpassword
      - MYSQL_DATABASE=eventdb
      - MYSQL_USER=user
      - MYSQL_PASSWORD=password
    ports:
      - "3306:3306"
    volumes:
      - db_data:/var/lib/mysql
    networks:
      - app-net

volumes:
  db_data:

networks:
  app-net:
    driver: bridge
```

## 5. Le Pipeline CI/CD avec Jenkins (Tâche 3)

Le pipeline sera défini dans un `Jenkinsfile` utilisant la syntaxe **Pipeline as Code** (Groovy). Il doit inclure toutes les étapes requises par l'énoncé.

**Fichier : `Jenkinsfile`**

```groovy
// Définition du pipeline
pipeline {
    // Exécuté sur un agent Docker (nécessite l'installation du plugin Docker Pipeline)
    agent {
        docker {
            image 'maven:3.8.6-openjdk-17'
            args '-v /root/.m2:/root/.m2' // Cache Maven
        }
    }

    // Variables d'environnement
    environment {
        // Remplacer par vos informations
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials') // ID de vos identifiants DockerHub dans Jenkins
        NEXUS_CREDENTIALS = credentials('nexus-credentials')       // ID de vos identifiants Nexus dans Jenkins
        SONAR_HOST_URL = 'http://sonarqube:9000'                   // URL de SonarQube (dans le réseau Docker)
        DOCKER_IMAGE = "VOTRE_DOCKERHUB_USER/event-app:${env.BUILD_NUMBER}"
    }

    stages {
        // 1. Récupération du projet de Git
        stage('Checkout') {
            steps {
                git url: 'https://github.com/VOTRE_USER/VOTRE_REPO.git', branch: 'main'
            }
        }

        // 2. Compilation du projet
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }

        // 3. Lancement des tests unitaires automatiques (JUnit)
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    // Publication des résultats des tests
                    junit '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        // 4. Lancement des tests de qualité de code (SonarQube)
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube-Server') { // Nom de la configuration SonarQube dans Jenkins
                    sh "mvn sonar:sonar -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.login=${SONAR_TOKEN}"
                }
            }
        }

        // 5. Attente de la Quality Gate de SonarQube
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    // Nécessite le plugin SonarQube Scanner
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // 6. Préparation de la version à distribuer et Mise en place sur Nexus
        stage('Artifact Upload to Nexus') {
            steps {
                // Utilisation du plugin Nexus Artifact Uploader ou configuration de Maven pour déployer
                // Ici, nous utilisons la configuration Maven standard (pom.xml doit être configuré)
                sh "mvn deploy -DskipTests -s settings.xml" // settings.xml doit contenir les infos Nexus
            }
        }

        // 7. Création de l’image qui contient le livrable Spring à partir du fichier DockerFile
        stage('Build Docker Image') {
            steps {
                script {
                    // Nécessite l'accès au démon Docker (via /var/run/docker.sock)
                    sh "docker build -t ${DOCKER_IMAGE} ."
                }
            }
        }

        // 8. Dépôt de l'image créée sur DockerHub
        stage('Push Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USER')]) {
                        sh "docker login -u ${DOCKER_USER} -p ${DOCKER_PASSWORD}"
                        sh "docker push ${DOCKER_IMAGE}"
                    }
                }
            }
        }

        // 9. Lancement simultané de l'image Spring Boot et MySQL (Déploiement CD)
        stage('Deploy with Docker Compose') {
            steps {
                // Cette étape simule le déploiement sur un serveur cible
                // En pratique, vous copieriez les fichiers docker-compose.yml et .env sur le serveur
                // et lanceriez la commande à distance (ex: via SSH ou Kubernetes)
                sh "docker-compose -f docker-compose.yml up -d"
            }
        }
    }
}
```

## 6. Correction des Problèmes Sonar (Tâche 4)

Cette étape est **manuelle** et itérative.

1.  Après le premier lancement du pipeline, SonarQube affichera les *smells*, les *bugs* et les *vulnerabilities*.
2.  **Action :** Analyser le rapport SonarQube (accessible via `http://localhost:9000`).
3.  **Action :** Modifier le code source de l'application pour corriger les problèmes (ex: complexité cyclomatique élevée, variables non utilisées, duplication de code).
4.  **Action :** Commiter et pousser les changements sur GitHub pour relancer le pipeline automatiquement (Remarque 2.a).

## 7. Tests Fonctionnels (Tâche 5)

Une fois l'application déployée via `docker-compose.yml` (accessible sur `http://localhost:8080`), il faut valider son bon fonctionnement.

**Action :** Utiliser **Postman** ou **Swagger** pour interagir avec l'API.

1.  **POST** : Ajouter un événement (ex: `POST /api/events` avec un corps JSON).
2.  **GET** : Récupérer l'événement ajouté (ex: `GET /api/events/{id}`).

## 8. Supervision (Tâche 6 - Partie Supervision)

La supervision des conteneurs est réalisée avec **Prometheus** (collecte) et **Grafana** (visualisation).

### 8.1. cAdvisor

Pour collecter les métriques des conteneurs, nous allons ajouter **cAdvisor** à notre infrastructure.

**Action :** Mettre à jour le fichier `docker-compose-infra.yml` pour inclure cAdvisor et s'assurer que Prometheus le *scrape*.

```yaml
# ... (Ajouter à la suite des autres services dans docker-compose-infra.yml)
  cadvisor:
    image: gcr.io/cadvisor/cadvisor:latest
    ports:
      - "8080:8080"
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:rw
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
    networks:
      - devops-net
```

### 8.2. Configuration Grafana

1.  Accéder à Grafana (`http://localhost:3000`).
2.  **Action :** Ajouter Prometheus comme source de données (URL : `http://prometheus:9090`).
3.  **Action :** Importer un tableau de bord pré-existant pour Docker/cAdvisor (ex: ID 1621 ou 193).

## 9. Conclusion et Documentation

Ce guide fournit le cadre technique complet pour réaliser le projet. La prochaine étape sera de générer les fichiers de configuration finaux et de les accompagner d'une documentation explicative plus formelle.
