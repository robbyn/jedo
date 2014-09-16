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

-- Initial data

INSERT INTO folders(PARENT_ID,NAME)
    VALUES(NULL,'root');
INSERT INTO folders(PARENT_ID,NAME)
    VALUES((SELECT ID FROM folders WHERE PARENT_ID IS NULL AND NAME='root'),'sub1');
INSERT INTO folders(PARENT_ID,NAME)
    VALUES((SELECT ID FROM folders WHERE PARENT_ID IS NULL AND NAME='root'),'sub2');