FROM node:20-slim

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        python3 python3-pip \
        php-cli php-curl php-mbstring php-xml php-zip \
        ruby \
        lua5.4 bash \
        curl ca-certificates unzip \
    && rm -rf /var/lib/apt/lists/*

# Composer (PHP's package manager) - not bundled with php-cli by default,
# unlike npm (bundled with Node) and gem (bundled with the ruby apt
# package).
RUN curl -sS https://getcomposer.org/installer | php -- --install-dir=/usr/local/bin --filename=composer

# Pre-baked packages - installed once here at image build time, so a run
# that just does `import numpy` or `require('lodash')` works instantly with
# no install step, no network call, and no new risk (see server.js's
# installPackages for the separate live-install path used for anything
# NOT in this list). --break-system-packages: Debian's PEP 668 protection
# is meant to stop you breaking a shared system Python on a real machine -
# irrelevant in a single-purpose sandbox container that's rebuilt from
# scratch on every deploy.
RUN pip3 install --no-cache-dir --break-system-packages numpy pandas requests pillow

# A shared, global-ish node_modules - NODE_PATH (set below) makes these
# resolve for any script's require() without a local npm install, the same
# way pip's site-packages already work globally for python3. The project
# dir can't itself be named "node_modules" - npm init infers a package name
# from the directory's basename and rejects that exact name as invalid.
RUN mkdir -p /opt/pkgs && cd /opt/pkgs \
    && npm init -y >/dev/null \
    && npm install --omit=dev lodash axios dayjs
ENV NODE_PATH=/opt/pkgs/node_modules

RUN gem install httparty --no-document

# PHP has no equivalent of a global site-packages/NODE_PATH - Composer's
# autoloader is always relative to a specific vendor/ directory. Building
# one shared vendor/ here once and copying it into each run's workDir (see
# server.js) is much cheaper than a fresh `composer require` every request.
RUN mkdir -p /opt/php-vendor && cd /opt/php-vendor \
    && composer require --no-interaction guzzlehttp/guzzle

RUN apt-get update && apt-get install -y --no-install-recommends openjdk-11-jdk kotlin && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev

COPY server.js ./

ENV PORT=8080
EXPOSE 8080
CMD ["node", "server.js"]
