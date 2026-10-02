FROM eclipse-temurin:8-jdk-jammy

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*

COPY python-ml/requirements.txt python-ml/requirements.txt
RUN python3 -m pip install --no-cache-dir -r python-ml/requirements.txt

COPY src/ src/
COPY web/ web/
COPY python-ml/ python-ml/

RUN mkdir -p out \
    && javac -d out $(find src/main/java -name '*.java')

EXPOSE 8080

CMD ["java", "-Dpython.command=python3", "-cp", "out", "edu.college.ridematcher.web.WebMain"]
