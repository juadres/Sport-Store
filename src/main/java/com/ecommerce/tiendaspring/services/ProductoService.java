package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.Producto;
import com.ecommerce.tiendaspring.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private StockNotifier stockNotifier;

    @Autowired
    private com.ecommerce.tiendaspring.repositories.CarritoItemRepository carritoItemRepository;

    /**
     * Obtiene todos los productos deportivos ordenados
     * Incluye cálculo de stock reservado en tiempo real
     */
    public List<Producto> obtenerTodosLosProductos() {
        return productoRepository.findAll(
            Sort.by(Sort.Direction.ASC, "orden")
        ).stream()
            .peek(p -> {
                Integer reservado = carritoItemRepository.sumCantidadByProductoId(p.getId());
                p.setStockReservado(reservado == null ? 0 : reservado);
            })
            .collect(Collectors.toList());
    }

    /**
     * Obtiene productos por categoría deportiva
     * @param categoria Categoría: uniformes, sudaderas, calzado, accesorios
     */
    public List<Producto> obtenerProductosPorCategoria(String categoria) {
        return productoRepository.findByCategoria(
                categoria,
                Sort.by(Sort.Direction.ASC, "orden")
        );
    }

    /**
     * Obtiene productos deportivos con stock disponible
     */
    public List<Producto> obtenerProductosEnStock() {
        return productoRepository.findByStockGreaterThan(
                0,
                Sort.by(Sort.Direction.ASC, "orden")
        );
    }

    public Optional<Producto> obtenerProductoPorId(Long id) {
        return productoRepository.findById(id);
    }

    public Producto guardarProducto(Producto producto) {
        return productoRepository.save(producto);
    }
    
    /**
     * Repone stock de productos deportivos
     * @param productoId ID del producto
     * @param cantidadAgregar Cantidad a agregar (máximo 30 unidades)
     */
    public Producto reponerStock(Long productoId, int cantidadAgregar) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        int cantidadReal = Math.min(cantidadAgregar, 30);
        int nuevoStock = (producto.getStock() == null ? 0 : producto.getStock()) + cantidadReal;
        producto.setStock(nuevoStock);
        Producto guardado = productoRepository.save(producto);

        try {
            stockNotifier.notificarCambioStock(guardado);
        } catch (Exception ignored) {}

        return guardado;
    }

    /**
     * Actualiza stock después de una venta
     */
    public void actualizarStock(Long productoId, int cantidadVendida) {
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            int nuevoStock = producto.getStock() - cantidadVendida;
            if (nuevoStock < 0) nuevoStock = 0;
            producto.setStock(nuevoStock);
            productoRepository.save(producto);
        }
    }

    /**
     * Obtiene productos deportivos disponibles (stock > 0)
     * Incluye cálculo de stock reservado en tiempo real
     */
    public List<Producto> obtenerProductosDisponibles() {
        return productoRepository.findAll(
                Sort.by(Sort.Direction.ASC, "orden")
        ).stream()
                .peek(p -> {
                    Integer reservado = carritoItemRepository.sumCantidadByProductoId(p.getId());
                    p.setStockReservado(reservado == null ? 0 : reservado);
                })
                .filter(p -> p.getStockDisponible() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene productos disponibles por categoría deportiva
     * @param categoria Categoría: uniformes, sudaderas, calzado, accesorios
     */
    public List<Producto> obtenerProductosDisponiblesPorCategoria(String categoria) {
        return productoRepository.findByCategoria(
                categoria,
                Sort.by(Sort.Direction.ASC, "orden")
        ).stream()
                .peek(p -> {
                    Integer reservado = carritoItemRepository.sumCantidadByProductoId(p.getId());
                    p.setStockReservado(reservado == null ? 0 : reservado);
                })
                .filter(p -> p.getStockDisponible() > 0)
                .collect(Collectors.toList());
    }
}