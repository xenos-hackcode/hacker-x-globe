FROM node:20-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        gcc g++ golang-go rustc default-jdk-headless curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV DOTNET_ROOT=/usr/share/dotnet
ENV PATH="${DOTNET_ROOT}:${PATH}"
RUN curl -sSL https://dot.net/v1/dotnet-install.sh -o /tmp/dotnet-install.sh \
    && bash /tmp/dotnet-install.sh --channel 8.0 --install-dir /usr/share/dotnet \
    && rm /tmp/dotnet-install.sh

ENV GOCACHE=/tmp/.cache/go-build
ENV GOPATH=/tmp/.cache/go-path

WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev

# Pre-scaffold + restore a minimal C# console project so each run only swaps Program.cs
RUN dotnet new console -o /app/csharp-template \
    && dotnet build /app/csharp-template -c Release

COPY server.js ./

ENV PORT=8080
EXPOSE 8080
CMD ["node", "server.js"]
