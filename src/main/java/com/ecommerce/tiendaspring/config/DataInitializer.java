package com.ecommerce.tiendaspring.config;

import com.ecommerce.tiendaspring.models.Producto;
import com.ecommerce.tiendaspring.models.Rol;
import com.ecommerce.tiendaspring.models.Usuario;
import com.ecommerce.tiendaspring.repositories.ProductoRepository;
import com.ecommerce.tiendaspring.repositories.RolRepository;
import com.ecommerce.tiendaspring.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        inicializarRolesYUsuarios();
        inicializarProductos();
    }

    private void inicializarRolesYUsuarios() {
        if (rolRepository.count() == 0) {
            Rol rolAdmin = new Rol("ROLE_ADMIN");
            Rol rolUser = new Rol("ROLE_USER");
            rolRepository.save(rolAdmin);
            rolRepository.save(rolUser);
            System.out.println("Roles creados: ROLE_ADMIN, ROLE_USER");
        }

        if (usuarioRepository.findByEmail("admin@tienda.com").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setEmail("admin@tienda.com");
            admin.setPassword(passwordEncoder.encode("admin123"));

            Rol rolAdmin = rolRepository.findByNombre("ROLE_ADMIN").orElseThrow();
            Rol rolUser = rolRepository.findByNombre("ROLE_USER").orElseThrow();

            admin.setRoles(Arrays.asList(rolAdmin, rolUser));
            usuarioRepository.save(admin);
            System.out.println("Usuario admin creado: admin@tienda.com / admin123");
        }

        if (usuarioRepository.findByEmail("user@tienda.com").isEmpty()) {
            Usuario user = new Usuario();
            user.setNombre("Usuario Normal");
            user.setEmail("user@tienda.com");
            user.setPassword(passwordEncoder.encode("user123"));

            Rol rolUser = rolRepository.findByNombre("ROLE_USER").orElseThrow();
            user.setRoles(Collections.singletonList(rolUser));

            usuarioRepository.save(user);
            System.out.println("Usuario normal creado: user@tienda.com / user123");
        }
    }

    private void inicializarProductos() {
        if (productoRepository.count() == 0) {

            // UNIFORMES FÚTBOL
            productoRepository.save(new Producto(
                "Camiseta Real Madrid 2024 Local",
                "Camiseta oficial temporada 2024, tejido transpirable y tecnología dry-fit.",
                new BigDecimal("149990"),
                15,
                "uniformes",
                "/images/uniforme1.jpeg",
                1
            ));

            productoRepository.save(new Producto(
                "Jersey Barcelona Edición Especial",
                "Diseño exclusivo con detalles en oro, 100% poliéster reciclado.",
                new BigDecimal("159990"),
                12,
                "uniformes",
                "/images/uniforme2.jpg",
                2
            ));

            productoRepository.save(new Producto(
                "Uniforme Selección Colombia",
                "Camiseta oficial de la selección, diseño tricolor y escudo bordado.",
                new BigDecimal("139990"),
                8,
                "uniformes",
                "/images/uniforme3.jpeg",
                3
            ));

            productoRepository.save(new Producto(
                "Camiseta Bayern Munich Visitante",
                "Color rosa neón, tecnología de ventilación avanzada.",
                new BigDecimal("144990"),
                10,
                "uniformes",
                "/images/uniforme4.jpeg",
                4
            ));

            // SUDADERAS
            productoRepository.save(new Producto(
                "Sudadera Nike Club Fleece",
                "Tejido fleece suave, capucha ajustable y bolsillo canguro.",
                new BigDecimal("89990"),
                18,
                "sudaderas",
                "/images/sudadera1.jpeg",
                5
            ));

            productoRepository.save(new Producto(
                "Hoodie Adidas Originals",
                "Diseño clásico, corte regular y logo Adidas bordado.",
                new BigDecimal("79990"),
                20,
                "sudaderas",
                "/images/sudadera2.jpeg",
                6
            ));

            productoRepository.save(new Producto(
                "Buzo Under Armour Sportstyle",
                "Material técnico, ideal para entrenamiento y uso casual.",
                new BigDecimal("94990"),
                14,
                "sudaderas",
                "/images/sudadera3.jpeg",
                7
            ));

            productoRepository.save(new Producto(
                "Sudadera Puma Training",
                "Corte moderno, tela absorbente de humedad.",
                new BigDecimal("69990"),
                16,
                "sudaderas",
                "/images/sudadera4.jpeg",
                8
            ));

            // CALZADO DEPORTIVO
            productoRepository.save(new Producto(
                "Zapatos Nike Air Max 270",
                "Amortiguación Air Max visible, diseño moderno y cómodo.",
                new BigDecimal("249990"),
                9,
                "calzado",
                "/images/calzado1.jpeg",
                9
            ));

            productoRepository.save(new Producto(
                "Tenis Adidas Ultraboost 5.0",
                "Tecnología Boost, ideal para running y uso diario.",
                new BigDecimal("299990"),
                7,
                "calzado",
                "/images/calzado2.jpeg",
                10
            ));

            productoRepository.save(new Producto(
                "Botines Nike Mercurial Vapor 15",
                "Para fútbol profesional, suela FG y diseño aerodinámico.",
                new BigDecimal("189990"),
                11,
                "calzado",
                "/images/calzado3.jpeg",
                11
            ));

            productoRepository.save(new Producto(
                "Zapatillas New Balance 1080v12",
                "Amortiguación Fresh Foam, para running de larga distancia.",
                new BigDecimal("219990"),
                8,
                "calzado",
                "/images/calzado4.jpeg",
                12
            ));

            // ACCESORIOS DEPORTIVOS
            productoRepository.save(new Producto(
                "Balón Fútbol Professional Nike",
                "Balón oficial tamaño 5, superficie termoadherida.",
                new BigDecimal("59990"),
                25,
                "accesorios",
                "/images/accesorio1.jpeg",
                13
            ));

            productoRepository.save(new Producto(
                "Guantes Portería Uhlsport",
                "Goma de látex profesional, máxima adherencia y protección.",
                new BigDecimal("89990"),
                13,
                "accesorios",
                "/images/accesorio2.jpeg",
                14
            ));

            productoRepository.save(new Producto(
                "Rodilleras Volleyball Mikasa",
                "Protección acolchada, ajuste seguro y material transpirable.",
                new BigDecimal("39990"),
                22,
                "accesorios",
                "/images/accesorio3.jpeg",
                15
            ));

            productoRepository.save(new Producto(
                "Muñequeras Deportivas Nike",
                "Pack de 3 muñequeras absorbentes, varios colores.",
                new BigDecimal("24990"),
                30,
                "accesorios",
                "/images/accesorio4.jpeg",
                16
            ));

            System.out.println("Productos deportivos creados exitosamente");
        }
    }
}