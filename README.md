### A framework for JdbcTemplate mapping xml sqls.

_**1. clone repo**_

    git clone https://github.com/987856377/mjt-spring-boot-starter.git
    
    mvn clean install

_**2. add dependency**_

    <dependency>
        <groupId>com.springboot</groupId>
        <artifactId>mjt-spring-boot-starter</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

_**3. use `@EnableMappingJdbcTemplate`**_

    default scan xml package: {"classpath*:/mapper/**/*.xml", "classpath*:/xml/**/*.xml", "classpath*:/mjt/**/*.xml"}
   
    example: @EnableMappingJdbcTemplate(baseLocations = "classpath:/xml/*.xml")

_**4. xml file**_
    
    <?xml version="1.0" encoding="UTF-8" ?>
    <namespace xmlns="http://localhost/schema/framework"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xsi:schemaLocation="http://localhost/schema/framework http://localhost/schema/framework/framework.xsd"
           mapper="com.springboot.provider.mjt.constants.Mapper">
    
        <sql id="selectById">
            select * from role where id = ? and title = ?
        </sql>
    
        <sql id="selectByUsername">
            select * from user where username = ?
        </sql>

    </namespace>


_**5. class file**_
    
    public class Mapper {

        public static final String selectById = "com.springboot.provider.mjt.constants.Mapper.selectById";

    }

_**6. usage**_

    JdbcOperations jdbcTemplate = com.springboot.mjt.proxy.JdbcOperationsProxy.getProxyInstance(${dsName});
    
    RowMapper<Role> rowMapper = new BeanPropertyRowMapper<>(Role.class);
    
    List<Role> roles = jdbcTemplate.query(Mapper.selectById, rowMapper, 1, "超级管理员");
