package main.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import main.entities.PortfolioSize;

import javax.sql.DataSource;

@Configuration
@MapperScan("main.repos")
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        
        // Configure MyBatis settings
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setUseGeneratedKeys(true);
        
        // Register custom type handlers
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        typeHandlerRegistry.register(PortfolioSize.class, new PortfolioSizeTypeHandler());
        
        sessionFactory.setConfiguration(configuration);
        
        return sessionFactory.getObject();
    }
}
