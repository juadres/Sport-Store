package com.ecommerce.tiendaspring.controllers;

import com.ecommerce.tiendaspring.models.CarritoItemDTO;
import com.ecommerce.tiendaspring.models.Producto;
import com.ecommerce.tiendaspring.services.ProductoService;
import com.ecommerce.tiendaspring.services.CarritoService;
import com.ecommerce.tiendaspring.services.ExternalAPIService;
import com.ecommerce.tiendaspring.services.StockService;
import com.ecommerce.tiendaspring.services.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import com.ecommerce.tiendaspring.models.Usuario;
import java.io.IOException;
import com.ecommerce.tiendaspring.models.Venta;
import com.ecommerce.tiendaspring.services.VentaService;
import com.ecommerce.tiendaspring.services.PdfService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@SessionAttributes("carrito")
public class TiendaController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private StockService stockService;

    @Autowired
    private ExternalAPIService externalAPIService;

    // ================= Inicializar carrito =================
    @ModelAttribute("carrito")
    public List<CarritoItemDTO> inicializarCarrito() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                List<CarritoItemDTO> carritoBD = carritoService.cargarCarritoUsuario(auth.getName());
                if (carritoBD != null && !carritoBD.isEmpty()) {
                    carritoService.sincronizarStockCarrito(carritoBD);
                    return carritoBD;
                }
            }
        } catch (Exception e) {
            System.out.println("Error cargando carrito desde BD: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // ================= Mostrar tienda =================
    @GetMapping("/tienda")
    public String mostrarTienda(@RequestParam(defaultValue = "todos") String categoria,
                                @ModelAttribute("carrito") List<CarritoItemDTO> carrito,
                                HttpSession session,
                                Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                String email = auth.getName();
                carritoService.guardarCarritoUsuario(email, carrito);
            
            Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email).orElse(null);
            model.addAttribute("usuario", usuario);
            
            }

            List<Producto> productos = "todos".equals(categoria)
                    ? productoService.obtenerProductosDisponibles()
                    : productoService.obtenerProductosDisponiblesPorCategoria(categoria);

            model.addAttribute("productos", productos);
            model.addAttribute("categoriaSeleccionada", categoria);

            return "tienda";

        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la tienda: " + e.getMessage());
            return "tienda";
        }
    }

    // ================= Agregar al carrito =================
    @PostMapping("/agregar-carrito")
    public String agregarAlCarrito(@RequestParam Long productoId,
                                   @RequestParam(defaultValue = "1") int cantidad,
                                   @RequestParam(defaultValue = "todos") String categoria,
                                   @ModelAttribute("carrito") List<CarritoItemDTO> carrito,
                                   Model model) {
        try {
            Producto producto = productoService.obtenerProductoPorId(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            CarritoItemDTO itemExistente = carrito.stream()
                    .filter(i -> i.getProducto().getId().equals(productoId))
                    .findFirst()
                    .orElse(null);

            if (itemExistente != null) {
                int cantidadAnterior = itemExistente.getCantidad();
                int nuevaCantidadTotal = cantidadAnterior + cantidad;
                if (!stockService.actualizarReserva(productoId, cantidadAnterior, nuevaCantidadTotal)) {
                    model.addAttribute("error", "No hay suficiente stock. Solo quedan " + producto.getStockDisponible() + " unidades.");
                    return "redirect:/tienda?categoria=" + categoria;
                }
                itemExistente.setCantidad(nuevaCantidadTotal);
                itemExistente.setProducto(producto);
            } else {
                if (!stockService.reservarStock(productoId, cantidad)) {
                    model.addAttribute("error", "No hay suficiente stock. Solo quedan " + producto.getStockDisponible() + " unidades.");
                    return "redirect:/tienda?categoria=" + categoria;
                }
                carrito.add(new CarritoItemDTO(producto, cantidad));
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                carritoService.guardarCarritoUsuario(auth.getName(), carrito);
            }

            model.addAttribute("success", "Producto agregado al carrito");

        } catch (Exception e) {
            model.addAttribute("error", "Error al agregar producto: " + e.getMessage());
        }

        return "redirect:/tienda?categoria=" + categoria;
    }

    // ================= Ver carrito =================
@GetMapping("/carrito")
public String verCarrito(@ModelAttribute("carrito") List<CarritoItemDTO> carrito, 
                         Model model,
                         HttpServletResponse response) {
    
    try {
        // Asegurar que response no esté comprometida
        if (!response.isCommitted()) {
            BigDecimal total = carrito.stream()
                    .map(CarritoItemDTO::getSubtotal)
                    .reduce(java.math.BigDecimal.ZERO, BigDecimal::add);
            
            // Obtener conversiones de moneda
            Map<String, String> currencyConversions = externalAPIService.getCurrencyConversions(total);
            model.addAttribute("currencyConversions", currencyConversions);
            
            model.addAttribute("total", total);
            return "carrito";
        }
    } catch (Exception e) {
        e.printStackTrace();
        // NO redirigir si response ya está comprometida
        if (!response.isCommitted()) {
            return "redirect:/tienda";
        }
    }
    return null; // Si response está comprometida, dejar que termine
}
    // ================= Actualizar carrito =================
    @PostMapping("/actualizar-carrito")
    public String actualizarCarrito(@RequestParam Long productoId,
                                    @RequestParam int cantidad,
                                    @ModelAttribute("carrito") List<CarritoItemDTO> carrito,
                                    Model model) {
        try {
            Iterator<CarritoItemDTO> it = carrito.iterator();
            while (it.hasNext()) {
                CarritoItemDTO item = it.next();
                if (item.getProducto().getId().equals(productoId)) {
                    if (cantidad <= 0) {
                        int cantidadAnterior = item.getCantidad();
                        stockService.liberarStock(productoId, cantidadAnterior);
                        it.remove();
                        model.addAttribute("success", "Producto eliminado");
                    } else {
                        int cantidadAnterior = item.getCantidad();
                        if (!stockService.actualizarReserva(productoId, cantidadAnterior, cantidad)) {
                            model.addAttribute("error", "No hay suficiente stock para actualizar cantidad");
                            return "redirect:/carrito";
                        }
                        item.setCantidad(cantidad);
                        model.addAttribute("success", "Cantidad actualizada");
                    }
                    break;
                }
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                carritoService.guardarCarritoUsuario(auth.getName(), carrito);
            }

        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar carrito: " + e.getMessage());
        }

        return "redirect:/carrito";
    }

    // ================= Eliminar item del carrito =================
    @PostMapping("/eliminar-carrito")
    public String eliminarCarrito(@RequestParam Long productoId,
                                 @ModelAttribute("carrito") List<CarritoItemDTO> carrito,
                                 Model model) {
        Iterator<CarritoItemDTO> it = carrito.iterator();
        while (it.hasNext()) {
            CarritoItemDTO item = it.next();
            if (item.getProducto().getId().equals(productoId)) {
                stockService.liberarStock(productoId, item.getCantidad());
                it.remove();
                break;
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            carritoService.guardarCarritoUsuario(auth.getName(), carrito);
        }

        model.addAttribute("success", "Producto eliminado");
        return "redirect:/carrito";
    }

    // ================= Vaciar carrito =================
    @GetMapping("/vaciar-carrito")
    public String vaciarCarrito(@ModelAttribute("carrito") List<CarritoItemDTO> carrito, Model model) {
        for (CarritoItemDTO item : carrito) {
            stockService.liberarStock(item.getProducto().getId(), item.getCantidad());
        }
        carrito.clear();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            carritoService.guardarCarritoUsuario(auth.getName(), carrito);
        }

        model.addAttribute("success", "Carrito vaciado");
        return "redirect:/carrito";
    }

    // ================= Procesar venta =================
    @PostMapping("/procesar-venta")
    public String procesarVenta(@ModelAttribute("carrito") List<CarritoItemDTO> carrito,
                               Model model) {
        try {
            if (carrito.isEmpty()) {
                model.addAttribute("error", "El carrito está vacío");
                return "redirect:/carrito";
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Venta venta = ventaService.procesarVenta(usuario, carrito);

            carrito.clear();

            carritoService.guardarCarritoUsuario(email, carrito);

            model.addAttribute("success", "¡Venta procesada exitosamente! Número de factura: " + venta.getNumeroFactura());
            model.addAttribute("venta", venta);
            return "confirmacion-venta";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar la venta: " + e.getMessage());
            return "redirect:/carrito";
        }
    }

    // ================= Descargar factura =================
    @GetMapping("/descargar-factura/{ventaId}")
    public void descargarFactura(@PathVariable Long ventaId, HttpServletResponse response) {
        try {
            Venta venta = ventaService.obtenerVentaPorId(ventaId);
            if (venta == null) {
                response.sendError(404, "Venta no encontrada");
                return;
            }

            byte[] pdf = pdfService.generarFacturaPdf(venta);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment; filename=factura_" + venta.getNumeroFactura() + ".pdf");
            response.setContentLength(pdf.length);

            response.getOutputStream().write(pdf);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(500, "Error al generar el PDF");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // ================= Mis compras =================
    @GetMapping("/mis-compras")
    public String misCompras(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<Venta> ventas = ventaService.obtenerVentasPorUsuario(usuario.getId());
            model.addAttribute("ventas", ventas);
            return "mis-compras";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar las compras: " + e.getMessage());
            return "mis-compras";
        }
    }
}