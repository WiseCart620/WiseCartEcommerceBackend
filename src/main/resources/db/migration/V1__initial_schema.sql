-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: wisecart_ecommerce
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `addresses`
--

DROP TABLE IF EXISTS `addresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `addresses` (
  `is_default` bit(1) DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `address_line1` varchar(255) NOT NULL,
  `address_line2` varchar(255) DEFAULT NULL,
  `address_type` varchar(255) DEFAULT NULL,
  `city` varchar(255) NOT NULL,
  `company_name` varchar(255) DEFAULT NULL,
  `country` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `phone` varchar(255) NOT NULL,
  `postal_code` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `tax_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1fa36y2oqhao3wgg2rw1pi459` (`user_id`),
  CONSTRAINT `FK1fa36y2oqhao3wgg2rw1pi459` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `announcement_banners`
--

DROP TABLE IF EXISTS `announcement_banners`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement_banners` (
  `active` bit(1) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `bg_color` varchar(20) DEFAULT NULL,
  `text_color` varchar(20) DEFAULT NULL,
  `text` varchar(500) NOT NULL,
  `link` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `app_settings`
--

DROP TABLE IF EXISTS `app_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_settings` (
  `buy_now_enabled` bit(1) NOT NULL,
  `cart_enabled` bit(1) NOT NULL,
  `free_shipping_threshold` decimal(10,2) NOT NULL,
  `vat_rate` decimal(5,4) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `store_email` varchar(255) DEFAULT NULL,
  `store_name` varchar(255) DEFAULT NULL,
  `store_phone` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cart_coupons`
--

DROP TABLE IF EXISTS `cart_coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_coupons` (
  `cart_id` bigint NOT NULL,
  `coupon_code` varchar(255) DEFAULT NULL,
  KEY `FK2sfwq6xb84hssvdvr9kbtx3cr` (`cart_id`),
  CONSTRAINT `FK2sfwq6xb84hssvdvr9kbtx3cr` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_items` (
  `addon_price` decimal(10,2) DEFAULT NULL,
  `gift_wrap` bit(1) DEFAULT NULL,
  `is_addon` bit(1) DEFAULT NULL,
  `original_price` decimal(10,2) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `quantity` int NOT NULL,
  `saved_for_later` bit(1) DEFAULT NULL,
  `addon_product_add_on_id` bigint DEFAULT NULL,
  `addon_product_id` bigint DEFAULT NULL,
  `addon_variation_id` bigint DEFAULT NULL,
  `cart_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `variation_id` bigint DEFAULT NULL,
  `gift_message` text,
  `notes` text,
  PRIMARY KEY (`id`),
  KEY `FKw8imvc8idgjds6ufcd35rb0x` (`addon_product_id`),
  KEY `FKta62yjd7o5cnctl3tik3k6ooc` (`addon_product_add_on_id`),
  KEY `FK9fi6ro29fpkqykvr3i8s6rikg` (`addon_variation_id`),
  KEY `FKpcttvuq4mxppo8sxggjtn5i2c` (`cart_id`),
  KEY `FK1re40cjegsfvw58xrkdp6bac6` (`product_id`),
  KEY `FKpqj1ixv9n5rka84aymuoluiql` (`variation_id`),
  CONSTRAINT `FK1re40cjegsfvw58xrkdp6bac6` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FK9fi6ro29fpkqykvr3i8s6rikg` FOREIGN KEY (`addon_variation_id`) REFERENCES `product_variations` (`id`),
  CONSTRAINT `FKpcttvuq4mxppo8sxggjtn5i2c` FOREIGN KEY (`cart_id`) REFERENCES `carts` (`id`),
  CONSTRAINT `FKpqj1ixv9n5rka84aymuoluiql` FOREIGN KEY (`variation_id`) REFERENCES `product_variations` (`id`),
  CONSTRAINT `FKta62yjd7o5cnctl3tik3k6ooc` FOREIGN KEY (`addon_product_add_on_id`) REFERENCES `product_add_ons` (`id`),
  CONSTRAINT `FKw8imvc8idgjds6ufcd35rb0x` FOREIGN KEY (`addon_product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `carts`
--

DROP TABLE IF EXISTS `carts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carts` (
  `coupon_discount_amount` decimal(10,2) DEFAULT NULL,
  `coupon_discount_percentage` decimal(38,2) DEFAULT NULL,
  `discount_amount` decimal(10,2) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_guest` bit(1) DEFAULT NULL,
  `shipping_amount` decimal(10,2) DEFAULT NULL,
  `subtotal` decimal(10,2) DEFAULT NULL,
  `tax_amount` decimal(10,2) DEFAULT NULL,
  `total` decimal(10,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_activity` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `currency_code` varchar(255) DEFAULT NULL,
  `session_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_64t7ox312pqal3p7fg9o503c2` (`user_id`),
  UNIQUE KEY `UK_1ihnigm9j28oe6n2qkws6q13q` (`session_id`),
  CONSTRAINT `FKb5o626f86h46m4s7ms6ginnop` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `active` bit(1) NOT NULL,
  `level` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `description` text,
  `image_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `slug` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_oul14ho7bctbefv8jywp5v3i2` (`slug`),
  KEY `FKsaok720gsu4u2wrgbk10b5n8d` (`parent_id`),
  CONSTRAINT `FKsaok720gsu4u2wrgbk10b5n8d` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contact_messages`
--

DROP TABLE IF EXISTS `contact_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contact_messages` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `subject` varchar(200) NOT NULL,
  `email` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `status` enum('OPEN','IN_PROGRESS','RESOLVED') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contact_replies`
--

DROP TABLE IF EXISTS `contact_replies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contact_replies` (
  `is_read` bit(1) NOT NULL,
  `contact_message_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `message` text NOT NULL,
  `sender_type` enum('ADMIN','CUSTOMER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjyslu401q44hjkpub5rj4i4oe` (`contact_message_id`),
  CONSTRAINT `FKjyslu401q44hjkpub5rj4i4oe` FOREIGN KEY (`contact_message_id`) REFERENCES `contact_messages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupon_applicable_categories`
--

DROP TABLE IF EXISTS `coupon_applicable_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_applicable_categories` (
  `category_id` bigint DEFAULT NULL,
  `coupon_id` bigint NOT NULL,
  KEY `FK6iulmrpitw08078d7rhn0xill` (`coupon_id`),
  CONSTRAINT `FK6iulmrpitw08078d7rhn0xill` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupon_applicable_products`
--

DROP TABLE IF EXISTS `coupon_applicable_products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_applicable_products` (
  `coupon_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL,
  KEY `FKh7wu7u49aufrv5ri1j65igx25` (`coupon_id`),
  CONSTRAINT `FKh7wu7u49aufrv5ri1j65igx25` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupon_combinable_with`
--

DROP TABLE IF EXISTS `coupon_combinable_with`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_combinable_with` (
  `combinable_coupon_id` bigint DEFAULT NULL,
  `coupon_id` bigint NOT NULL,
  KEY `FKs7hq481r3tfewa66gfaly2cp5` (`coupon_id`),
  CONSTRAINT `FKs7hq481r3tfewa66gfaly2cp5` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupon_usage`
--

DROP TABLE IF EXISTS `coupon_usage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_usage` (
  `coupon_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint DEFAULT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKof9h3rw1vu403j5dbehr45ts2` (`coupon_id`),
  KEY `FKejqwoh58dj8l7apjqo75crp88` (`order_id`),
  KEY `FKcsx5pbxjnp6jvuveb3lxyklhe` (`user_id`),
  CONSTRAINT `FKcsx5pbxjnp6jvuveb3lxyklhe` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKejqwoh58dj8l7apjqo75crp88` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKof9h3rw1vu403j5dbehr45ts2` FOREIGN KEY (`coupon_id`) REFERENCES `coupons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coupons`
--

DROP TABLE IF EXISTS `coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupons` (
  `current_usage_count` int DEFAULT NULL,
  `discount_value` decimal(10,2) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_combinable` bit(1) NOT NULL,
  `max_usage_count` int DEFAULT NULL,
  `max_usage_per_user` int DEFAULT NULL,
  `maximum_discount_amount` decimal(10,2) DEFAULT NULL,
  `minimum_product_quantity` int DEFAULT NULL,
  `minimum_purchase_amount` decimal(10,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `expiration_date` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `start_date` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `code` varchar(255) NOT NULL,
  `description` text,
  `type` enum('PERCENTAGE','FIXED_AMOUNT','FREE_SHIPPING') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_eplt0kkm9yf2of2lnx6c1oy9b` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flash_express_settings`
--

DROP TABLE IF EXISTS `flash_express_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flash_express_settings` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `base_url` varchar(255) NOT NULL,
  `mch_id` varchar(255) NOT NULL,
  `secret_key` varchar(255) NOT NULL,
  `src_city_name` varchar(255) NOT NULL,
  `src_detail_address` text NOT NULL,
  `src_name` varchar(255) NOT NULL,
  `src_phone` varchar(255) NOT NULL,
  `src_postal_code` varchar(255) NOT NULL,
  `src_province_name` varchar(255) NOT NULL,
  `warehouse_no` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hero_banners`
--

DROP TABLE IF EXISTS `hero_banners`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hero_banners` (
  `active` bit(1) NOT NULL,
  `display_order` int NOT NULL,
  `overlay_opacity` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `text_color` varchar(10) DEFAULT NULL,
  `button_text` varchar(50) DEFAULT NULL,
  `badge` varchar(100) DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `subtitle` varchar(200) DEFAULT NULL,
  `button_link` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `homepage_section_configs`
--

DROP TABLE IF EXISTS `homepage_section_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homepage_section_configs` (
  `active` bit(1) NOT NULL,
  `display_order` int NOT NULL,
  `product_limit` int NOT NULL,
  `section_banner_overlay` int DEFAULT NULL,
  `show_timer` bit(1) NOT NULL,
  `category_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `timer_ends_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `mode` varchar(20) NOT NULL,
  `section_key` varchar(50) NOT NULL,
  `timer_label` varchar(60) DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `section_banner_title` varchar(200) DEFAULT NULL,
  `subtitle` varchar(200) DEFAULT NULL,
  `section_banner_description` varchar(500) DEFAULT NULL,
  `section_banner_link` varchar(500) DEFAULT NULL,
  `section_banner_url` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_efds87w0utugrocjxv6uickvk` (`section_key`),
  KEY `FKa5m0bo6tfp1yitluf3qs4j0qh` (`category_id`),
  CONSTRAINT `FKa5m0bo6tfp1yitluf3qs4j0qh` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
  CONSTRAINT `homepage_section_configs_chk_1` CHECK ((`mode` in (_utf8mb4'AUTO',_utf8mb4'MANUAL',_utf8mb4'CATEGORY')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `homepage_section_products`
--

DROP TABLE IF EXISTS `homepage_section_products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `homepage_section_products` (
  `display_order` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `section_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKj217wi911xsa8gsikf0db5tkv` (`section_id`,`product_id`),
  KEY `FKmswdk03fnl1sr4mavbf7d61y9` (`product_id`),
  CONSTRAINT `FKaywhaa7jn11rmtvi2qo4447w7` FOREIGN KEY (`section_id`) REFERENCES `homepage_section_configs` (`id`),
  CONSTRAINT `FKmswdk03fnl1sr4mavbf7d61y9` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `newsletter_subscribers`
--

DROP TABLE IF EXISTS `newsletter_subscribers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `newsletter_subscribers` (
  `active` bit(1) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subscribed_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `source` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_qqdefkuupml4s7190ettcy6jy` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `is_read` bit(1) NOT NULL,
  `total_items` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reference_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `message` text NOT NULL,
  `reference_type` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9y21adhxn0ayjhfocscqox7bh` (`user_id`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `addon_price` decimal(10,2) DEFAULT NULL,
  `is_addon` bit(1) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `quantity` int NOT NULL,
  `subtotal` decimal(10,2) NOT NULL,
  `addon_product_add_on_id` bigint DEFAULT NULL,
  `addon_product_id` bigint DEFAULT NULL,
  `addon_variation_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `variation_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7mfmb1jcdrq14j92t5f5s5e9r` (`addon_product_id`),
  KEY `FKigtk2f6r4xftppt96dj9uj3pn` (`addon_product_add_on_id`),
  KEY `FKgt96er6jlupjbbqwvsindcbli` (`addon_variation_id`),
  KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
  KEY `FKocimc7dtr037rh4ls4l95nlfi` (`product_id`),
  KEY `FKlasrvx6ygx12jyqa5bl0pcss2` (`variation_id`),
  CONSTRAINT `FK7mfmb1jcdrq14j92t5f5s5e9r` FOREIGN KEY (`addon_product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKgt96er6jlupjbbqwvsindcbli` FOREIGN KEY (`addon_variation_id`) REFERENCES `product_variations` (`id`),
  CONSTRAINT `FKigtk2f6r4xftppt96dj9uj3pn` FOREIGN KEY (`addon_product_add_on_id`) REFERENCES `product_add_ons` (`id`),
  CONSTRAINT `FKlasrvx6ygx12jyqa5bl0pcss2` FOREIGN KEY (`variation_id`) REFERENCES `product_variations` (`id`),
  CONSTRAINT `FKocimc7dtr037rh4ls4l95nlfi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `discount_amount` decimal(10,2) DEFAULT NULL,
  `final_amount` decimal(10,2) NOT NULL,
  `flash_state` int DEFAULT NULL,
  `shipping_amount` decimal(10,2) DEFAULT NULL,
  `tax_amount` decimal(10,2) DEFAULT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `billing_address_id` bigint DEFAULT NULL,
  `cancelled_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  `estimated_delivery` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shipping_address_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `coupon_code` varchar(255) DEFAULT NULL,
  `guest_email` varchar(255) DEFAULT NULL,
  `guest_first_name` varchar(255) DEFAULT NULL,
  `guest_last_name` varchar(255) DEFAULT NULL,
  `guest_phone` varchar(255) DEFAULT NULL,
  `maya_payment_method` varchar(255) DEFAULT NULL,
  `notes` text,
  `order_number` varchar(255) NOT NULL,
  `payment_method` varchar(255) DEFAULT NULL,
  `shipping_carrier` varchar(255) DEFAULT NULL,
  `tracking_number` varchar(255) DEFAULT NULL,
  `payment_status` enum('PENDING','COMPLETED','FAILED','REFUNDED','PARTIALLY_REFUNDED','CANCELLED') DEFAULT NULL,
  `status` enum('PENDING','PROCESSING','SHIPPED','OUT_FOR_DELIVERY','DELIVERED','CANCELLED','REFUNDED','RETURNED','FAILED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_nthkiu7pgmnqnu86i2jyoe2v7` (`order_number`),
  KEY `FK66jolu65brloux12yi37qy3ky` (`billing_address_id`),
  KEY `FKmk6q95x8ffidq82wlqjaq7sqc` (`shipping_address_id`),
  KEY `FK32ql8ubntj5uh44ph9659tiih` (`user_id`),
  CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK66jolu65brloux12yi37qy3ky` FOREIGN KEY (`billing_address_id`) REFERENCES `addresses` (`id`),
  CONSTRAINT `FKmk6q95x8ffidq82wlqjaq7sqc` FOREIGN KEY (`shipping_address_id`) REFERENCES `addresses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `amount` decimal(10,2) NOT NULL,
  `refund_amount` decimal(10,2) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `failed_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `refunded_at` datetime(6) DEFAULT NULL,
  `card_brand` varchar(255) DEFAULT NULL,
  `card_last_four` varchar(255) DEFAULT NULL,
  `maya_payment_id` varchar(255) DEFAULT NULL,
  `maya_transaction_reference` varchar(255) DEFAULT NULL,
  `payer_email` varchar(255) DEFAULT NULL,
  `payer_name` varchar(255) DEFAULT NULL,
  `payment_gateway` varchar(255) DEFAULT NULL,
  `payment_method` varchar(255) NOT NULL,
  `refund_reason` varchar(255) DEFAULT NULL,
  `transaction_id` varchar(255) DEFAULT NULL,
  `gateway_response` tinytext,
  `status` enum('PENDING','COMPLETED','FAILED','REFUNDED','PARTIALLY_REFUNDED','CANCELLED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_lryndveuwa4k5qthti0pkmtlx` (`transaction_id`),
  KEY `FK81gagumt0r8y3rmudcgpbk42l` (`order_id`),
  CONSTRAINT `FK81gagumt0r8y3rmudcgpbk42l` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pending_checkouts`
--

DROP TABLE IF EXISTS `pending_checkouts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pending_checkouts` (
  `amount` decimal(38,2) NOT NULL,
  `express_category` int DEFAULT NULL,
  `shipping_fee` decimal(38,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint DEFAULT NULL,
  `shipping_address_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `maya_payment_id` varchar(100) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `address_line1` varchar(255) DEFAULT NULL,
  `address_line2` varchar(255) DEFAULT NULL,
  `checkout_ref` varchar(255) NOT NULL,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `coupon_code` varchar(255) DEFAULT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `guest_email` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `maya_checkout_id` varchar(255) DEFAULT NULL,
  `maya_checkout_url` varchar(255) DEFAULT NULL,
  `maya_transaction_reference` varchar(255) DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `payment_method` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `postal_code` varchar(255) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `verification_token` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','PROCESSING','COMPLETED','FAILED','EXPIRED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_4tiwbd6tx4pv6x19m6qk0j42x` (`checkout_ref`),
  KEY `FKq9gtq9lga5mw64llxyu80xmk8` (`user_id`),
  CONSTRAINT `FKq9gtq9lga5mw64llxyu80xmk8` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_add_ons`
--

DROP TABLE IF EXISTS `product_add_ons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_add_ons` (
  `display_order` int DEFAULT NULL,
  `special_price` decimal(10,2) DEFAULT NULL,
  `add_on_product_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8ievlires1ppgr7dmevnejasu` (`product_id`,`add_on_product_id`),
  KEY `FKn6i509obw2k345dl9y6nv6qpg` (`add_on_product_id`),
  CONSTRAINT `FKkk268ojpn7g0anf1ynk1sgav8` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKn6i509obw2k345dl9y6nv6qpg` FOREIGN KEY (`add_on_product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_images`
--

DROP TABLE IF EXISTS `product_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_images` (
  `display_order` int DEFAULT NULL,
  `is_primary` bit(1) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `alt_text` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) NOT NULL,
  `image_type` enum('GALLERY','DESCRIPTION') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqnq71xsohugpqwf3c9gxmsuy` (`product_id`),
  CONSTRAINT `FKqnq71xsohugpqwf3c9gxmsuy` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_recommendations`
--

DROP TABLE IF EXISTS `product_recommendations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_recommendations` (
  `product_id` bigint NOT NULL,
  `recommended_product_id` bigint NOT NULL,
  KEY `FKk5fwog70bi3gc7gpjlg6kn26t` (`recommended_product_id`),
  KEY `FK8s9f99xpilkhreo94t692lwhs` (`product_id`),
  CONSTRAINT `FK8s9f99xpilkhreo94t692lwhs` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKk5fwog70bi3gc7gpjlg6kn26t` FOREIGN KEY (`recommended_product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_variations`
--

DROP TABLE IF EXISTS `product_variations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_variations` (
  `active` bit(1) NOT NULL,
  `discount` decimal(5,2) DEFAULT NULL,
  `height_cm` decimal(8,2) DEFAULT NULL,
  `length_cm` decimal(8,2) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `stock_quantity` int NOT NULL,
  `weight_kg` decimal(8,3) DEFAULT NULL,
  `width_cm` decimal(8,2) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `sku` varchar(255) DEFAULT NULL,
  `upc` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_c5bawwopev2megdxlx259u5k7` (`sku`),
  UNIQUE KEY `UK_batu119n3f9xwgh4atq9x2htf` (`upc`),
  KEY `FKtopipc5x691v9hjrughkfn6ff` (`product_id`),
  CONSTRAINT `FKtopipc5x691v9hjrughkfn6ff` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `active` bit(1) NOT NULL,
  `discount` decimal(5,2) DEFAULT NULL,
  `height_cm` decimal(38,2) DEFAULT NULL,
  `is_featured` bit(1) DEFAULT NULL,
  `length_cm` decimal(38,2) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `rating` decimal(3,2) DEFAULT NULL,
  `review_count` int DEFAULT NULL,
  `sold_count` int DEFAULT NULL,
  `stock_quantity` int DEFAULT NULL,
  `view_count` int DEFAULT NULL,
  `weight_kg` decimal(8,3) DEFAULT NULL,
  `width_cm` decimal(38,2) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recommendation_category_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `label` varchar(500) DEFAULT NULL,
  `lazada_url` varchar(2048) DEFAULT NULL,
  `shopee_url` varchar(2048) DEFAULT NULL,
  `description` text,
  `image_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `sku` varchar(255) DEFAULT NULL,
  `upc` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_fhmd06dsmj6k0n90swsh8ie9g` (`sku`),
  UNIQUE KEY `UK_3nbbua79yw9k15ov20igt3rh1` (`upc`),
  KEY `FKog2rp4qthbtt2lfyhfo32lsw9` (`category_id`),
  KEY `FKowk1xgc3pf12dlux932fxijed` (`recommendation_category_id`),
  CONSTRAINT `FKog2rp4qthbtt2lfyhfo32lsw9` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
  CONSTRAINT `FKowk1xgc3pf12dlux932fxijed` FOREIGN KEY (`recommendation_category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `promo_cards`
--

DROP TABLE IF EXISTS `promo_cards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promo_cards` (
  `active` bit(1) NOT NULL,
  `display_order` int DEFAULT NULL,
  `overlay_opacity` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `button_text` varchar(255) DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `link_url` varchar(255) DEFAULT NULL,
  `subtitle` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review_images`
--

DROP TABLE IF EXISTS `review_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_images` (
  `display_order` int DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_id` bigint NOT NULL,
  `image_url` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3aayo5bjciyemf3bvvt987hkr` (`review_id`),
  CONSTRAINT `FK3aayo5bjciyemf3bvvt987hkr` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `active` bit(1) NOT NULL,
  `helpful_count` int DEFAULT NULL,
  `is_reported` bit(1) DEFAULT NULL,
  `is_verified_purchase` bit(1) DEFAULT NULL,
  `rating` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `comment` text,
  `report_reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqwgq1lxgahsxdspnwqfac6sv6` (`order_id`),
  KEY `FKpl51cejpw4gy5swfar8br9ngi` (`product_id`),
  KEY `FKcgy7qjc1r99dp117y9en6lxye` (`user_id`),
  CONSTRAINT `FKcgy7qjc1r99dp117y9en6lxye` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKpl51cejpw4gy5swfar8br9ngi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKqwgq1lxgahsxdspnwqfac6sv6` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `saved_cart_items`
--

DROP TABLE IF EXISTS `saved_cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `saved_cart_items` (
  `price` decimal(10,2) NOT NULL,
  `quantity` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL,
  `saved_cart_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKaw6iawv0mb41q479c4998iwjr` (`product_id`),
  KEY `FK3sdn3vwmr46wdqrywmut709py` (`saved_cart_id`),
  CONSTRAINT `FK3sdn3vwmr46wdqrywmut709py` FOREIGN KEY (`saved_cart_id`) REFERENCES `saved_carts` (`id`),
  CONSTRAINT `FKaw6iawv0mb41q479c4998iwjr` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `saved_carts`
--

DROP TABLE IF EXISTS `saved_carts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `saved_carts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `saved_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmv0jyxs0l8md84wt8nbaqnevd` (`user_id`),
  CONSTRAINT `FKmv0jyxs0l8md84wt8nbaqnevd` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `testimonials`
--

DROP TABLE IF EXISTS `testimonials`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `testimonials` (
  `active` bit(1) NOT NULL,
  `display_order` int NOT NULL,
  `rating` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `customer_name` varchar(100) NOT NULL,
  `customer_title` varchar(100) DEFAULT NULL,
  `product_name` varchar(150) DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `review` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_wishlist`
--

DROP TABLE IF EXISTS `user_wishlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_wishlist` (
  `product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`product_id`,`user_id`),
  KEY `FKxfiwf0ov64o4j979puogxloy` (`user_id`),
  CONSTRAINT `FKme2h6wt7w92ypitpwsol95j73` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `FKxfiwf0ov64o4j979puogxloy` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `email_verified` bit(1) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reset_token_expiry` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `reset_token` varchar(255) DEFAULT NULL,
  `verification_token` varchar(255) DEFAULT NULL,
  `role` enum('ADMIN','CUSTOMER','SELLER','MODERATOR','GUEST') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKdu5v5sr43g5bfnji4vb8hg5s3` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-21  6:04:23
