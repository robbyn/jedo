CREATE TABLE folders (
    ID INT NOT NULL IDENTITY,
    PARENT_ID INT,
    NAME VARCHAR(256) NOT NULL,

    FOREIGN KEY (PARENT_ID) REFERENCES folders(ID)
);

CREATE UNIQUE INDEX folders_parent_name ON folders(PARENT_ID,NAME);

CREATE TABLE pictures (
    ID INT NOT NULL IDENTITY,
    FOLDER_ID INT NOT NULL,
    NAME VARCHAR(256) NOT NULL,
    TIMESTAMP TIMESTAMP NOT NULL,
    WIDTH INT NOT NULL,
    HEIGHT INT NOT NULL,
    LATITUDE DOUBLE,
    LONGITUDE DOUBLE,
    ALTITUDE DOUBLE,

    FOREIGN KEY (FOLDER_ID) REFERENCES folders(ID)
);

CREATE UNIQUE INDEX pictures_folder_name ON pictures(FOLDER_ID,NAME);

CREATE TABLE picture_tags (
    PICTURE_ID INT NOT NULL,
    INDEX INT NOT NULL,
    NAME VARCHAR(256) NOT NULL,

    PRIMARY KEY (PICTURE_ID,INDEX),
    FOREIGN KEY (PICTURE_ID) REFERENCES pictures(ID)
);

CREATE INDEX picture_tags_name ON picture_tags(NAME);

CREATE TABLE picture_descriptions (
    PICTURE_ID INT NOT NULL,
    LANGUAGE VARCHAR(4) NOT NULL,
    DESCRIPTION TEXT NOT NULL,

    PRIMARY KEY (PICTURE_ID,LANGUAGE),
    FOREIGN KEY (PICTURE_ID) REFERENCES pictures(ID)
);

-- Initial data

INSERT INTO folders(PARENT_ID,NAME)
    VALUES(NULL,'root');
INSERT INTO folders(PARENT_ID,NAME)
    VALUES((SELECT ID FROM folders WHERE PARENT_ID IS NULL AND NAME='root'),'sub1');
INSERT INTO folders(PARENT_ID,NAME)
    VALUES((SELECT ID FROM folders WHERE PARENT_ID IS NULL AND NAME='root'),'sub2');
