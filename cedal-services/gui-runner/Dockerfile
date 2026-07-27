FROM node:20-slim

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
        python3 python3-tk \
        xvfb x11vnc \
        novnc websockify \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev

COPY server.js ./

ENV PORT=8080
EXPOSE 8080
CMD ["node", "server.js"]
