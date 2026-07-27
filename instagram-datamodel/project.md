This project is a proof of concept of data modeling for a simple instagram like application. 
It will use Spring data for entity modeling and data access. 
It will use in memory databases or mocks for the datasources. 
It will model the following entities:

PostgreSQL Database:
- Users Table: Stores user account details.
  - id (Primary Key)
  - username
  - email
  - password_hash
- Posts Table: Stores metadata related to posts.
    - id (Primary Key)
    - user_id (Foreign Key to Users Table)
    - likes_count
- Media Table: Stores photo/video metadata, but not the actual files.
    - id (Primary Key)
    - post_id (Foreign Key to Posts Table)
    - media_type (photo/video)
    - media_url
- Comments Table: Stores post comments.
    - id (Primary Key)
    - post_id (Foreign Key to Posts Table)
    - user_id (Foreign Key to Users Table)
    - comment_text
- Shares Table: Stores post shares.
    - id (Primary Key)
    - post_id (Foreign Key to Posts Table)
    - user_id (Foreign Key to Users Table)
- Followers Table: Maintains the follow/unfollow relationship. Stores engagement score from followers to help with ranking posts in the feed.
  - id (Primary Key)
  - followee_id (Foreign Key to Users Table)
  - created_at
  
Redis:
- User Feed Cache: Caches the feed for each user to improve performance and reduce database load.

Neo4j:
- Social Graph: Models the relationships between users, such as followers and followings

Elasticsearch:
- Search Index: Indexes posts, users, and comments to enable fast search capabilities across the application.
- Hashtags Index: Indexes hashtags used in posts to facilitate trending topics and hashtag searches.