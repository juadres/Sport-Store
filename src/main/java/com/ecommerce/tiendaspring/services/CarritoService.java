package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.*;
import com.ecommerce.tiendaspring.repositories.CarritoRepository;
import com.ecommerce.tiendaspring.repositories.CarritoItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CarritoService {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private CarritoItemRepository carritoItemRepository;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private StockService stockService;

    // ================= Obtener o crear carrito para un usuario =================
    public Carrito obtenerCarritoUsuario(Usuario usuario) {
        return carritoRepository.findByUsuario(usuario)
                .orElseGet(() -> {
                    Carrito nuevoCarrito = new Carrito(usuario);
                    return carritoRepository.save(nuevoCarrito);
                });
    }

    // ================= Convertir Carrito persistente a DTO =================
    public List<CarritoItemDTO> convertirACarritoDTO(Carrito carrito) {
        List<CarritoItemDTO> carritoDTO = new ArrayList<>();
        for (CarritoItem item : carrito.getItems()) {
            carritoDTO.add(new CarritoItemDTO(item.getProducto(), item.getCantidad()));
        }
        return carritoDTO;
    }

    // ================= Actualizar Carrito desde DTO =================
    public void actualizarCarritoDesdeDTO(Usuario usuario, List<CarritoItemDTO> carritoDTO) {
        Carrito carrito = obtenerCarritoUsuario(usuario);

        // Limpiar items existentes
        carrito.getItems().clear();
        carritoItemRepository.deleteByCarritoId(carrito.getId());

        // Agregar nuevos items desde DTO
        for (CarritoItemDTO itemDTO : carritoDTO) {
            CarritoItem item = new CarritoItem(carrito, itemDTO.getProducto(), itemDTO.getCantidad());
            carrito.agregarItem(item);
        }

        carritoRepository.save(carrito);
    }

    // ================= Cargar carrito desde BD =================
    public List<CarritoItemDTO> cargarCarritoUsuario(String email) {
        System.out.println("Cargando carrito para usuario: " + email);

        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorEmail(email);
        if (usuarioOpt.isPresent()) {
            Carrito carrito = obtenerCarritoUsuario(usuarioOpt.get());
            List<CarritoItemDTO> carritoDTO = convertirACarritoDTO(carrito);
            System.out.println("Carrito cargado: " + carritoDTO.size() + " items");
            return carritoDTO;
        }

        System.out.println("Usuario no encontrado: " + email);
        return new ArrayList<>();
    }

    // ================= Guardar carrito DTO en BD =================
    public void guardarCarritoUsuario(String email, List<CarritoItemDTO> carritoDTO) {
        System.out.println("Guardando carrito para usuario: " + email + " - Items: " + carritoDTO.size());

        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorEmail(email);
        if (usuarioOpt.isPresent()) {
            actualizarCarritoDesdeDTO(usuarioOpt.get(), carritoDTO);
            System.out.println("Carrito guardado exitosamente");
            // Después de guardar en BD, emitir actualizaciones de stock para cada producto
            for (CarritoItemDTO item : carritoDTO) {
                try {
                    stockService.broadcastStockUpdate(item.getProducto().getId());
                } catch (Exception e) {
                    System.out.println("Error broadcasting stock update: " + e.getMessage());
                }
            }
        } else {
            System.out.println("No se pudo guardar carrito - usuario no encontrado");
        }
    }

    // ================= Sincronizar stock entre sesión y BD =================
    public void sincronizarStockCarrito(List<CarritoItemDTO> carritoDTO) {
        for (CarritoItemDTO item : carritoDTO) {
            int stockDisponible = item.getProducto().getStockDisponible();
            if (item.getCantidad() > stockDisponible) {
                System.out.println("Ajustando cantidad de " + item.getProducto().getNombre() +
                        " de " + item.getCantidad() + " a " + stockDisponible);
                item.setCantidad(stockDisponible);
            }
        }
    }

    // ================= NUEVO: Limpiar carrito de usuario =================
    public void limpiarCarritoUsuario(String email) {
        System.out.println("=== Limpiando carrito para usuario: " + email + " ===");
        
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorEmail(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            // 1. Obtener el carrito actual
            Carrito carrito = obtenerCarritoUsuario(usuario);
            
            System.out.println("Carrito encontrado con " + carrito.getItems().size() + " items");
            
            // 2. Liberar stock reservado para cada item
            for (CarritoItem item : carrito.getItems()) {
                try {
                    System.out.println("Liberando stock: Producto " + item.getProducto().getId() + 
                                     ", Cantidad: " + item.getCantidad());
                    stockService.liberarStock(item.getProducto().getId(), item.getCantidad());
                } catch (Exception e) {
                    System.out.println("Error liberando stock para producto " + 
                                     item.getProducto().getId() + ": " + e.getMessage());
                }
            }
            
            // 3. Eliminar todos los items del carrito
            int itemsEliminados = carrito.getItems().size();
            carrito.getItems().clear();
            
            // 4. Eliminar de la base de datos
            carritoItemRepository.deleteByCarritoId(carrito.getId());
            
            // 5. Guardar carrito vacío
            carritoRepository.save(carrito);
            
            System.out.println("Carrito limpiado exitosamente. " + itemsEliminados + " items eliminados.");
            
        } else {
            System.out.println("Usuario no encontrado: " + email);
        }
    }

    // ================= NUEVO: Vaciar carrito temporal (para sesión) =================
    public void vaciarCarritoSesion(String email, List<CarritoItemDTO> carritoSesion) {
        System.out.println("=== Vaciar carrito de sesión para: " + email + " ===");
        
        if (carritoSesion != null && !carritoSesion.isEmpty()) {
            // 1. Liberar stock de los items en sesión
            for (CarritoItemDTO item : carritoSesion) {
                try {
                    stockService.liberarStock(item.getProducto().getId(), item.getCantidad());
                } catch (Exception e) {
                    System.out.println("Error liberando stock de sesión: " + e.getMessage());
                }
            }
            
            // 2. También limpiar carrito en BD
            limpiarCarritoUsuario(email);
        }
    }
}