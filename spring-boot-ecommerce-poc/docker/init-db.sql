-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS product_schema;
CREATE SCHEMA IF NOT EXISTS order_schema;
CREATE SCHEMA IF NOT EXISTS inventory_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;

-- Grant usage to the ecommerce user
GRANT ALL ON SCHEMA product_schema TO ecommerce;
GRANT ALL ON SCHEMA order_schema TO ecommerce;
GRANT ALL ON SCHEMA inventory_schema TO ecommerce;
GRANT ALL ON SCHEMA payment_schema TO ecommerce;
