DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS history;
DROP TABLE IF EXISTS layer_manifest;
DROP TABLE IF EXISTS layers;
DROP TABLE IF EXISTS manifests;

CREATE TABLE IF NOT EXISTS manifests (
    image_digest text,
    manifest_digest text UNIQUE,
    container text,
    created text,
    docker_version text,
    PRIMARY KEY (image_digest)
);

CREATE TABLE IF NOT EXISTS layers (
    digest text,
    size int,
    last_modified text,
    PRIMARY KEY (digest)
);

CREATE TABLE IF NOT EXISTS layer_manifest (
    layer_digest text,
    image_digest text,
    PRIMARY KEY (layer_digest, image_digest),
    FOREIGN KEY (image_digest) REFERENCES manifests(image_digest),
    FOREIGN KEY (layer_digest) REFERENCES layers(digest)

);

CREATE TABLE IF NOT EXISTS history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    layer_digest text,
    created text,
    created_by text,
    image_digest text,
    FOREIGN KEY (image_digest) REFERENCES manifests(image_digest)

);

CREATE TABLE IF NOT EXISTS images (
    name text,
    tag text,
    digest text,
    PRIMARY KEY (name, tag),
    FOREIGN KEY (digest) REFERENCES manifests(image_digest)
);