CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    raw_user_meta_data VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS products (
    product_id INT PRIMARY KEY,
    name VARCHAR(255),
    avg_rating DOUBLE,
    description VARCHAR(255),
    image VARCHAR(255),
    link VARCHAR(255),
    category VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS product_reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT,
    review_rating INT,
    review_desc VARCHAR(255),
    uid VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS relations (
    uid VARCHAR(36) NOT NULL,
    following VARCHAR(36) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_similarity_cache (
    uuid1 VARCHAR(36) NOT NULL,
    uuid2 VARCHAR(36) NOT NULL,
    similarity_score DOUBLE,
    product_similarity DOUBLE,
    rating_similarity DOUBLE,
    last_calculated TIMESTAMP,
    PRIMARY KEY (uuid1, uuid2)
);

