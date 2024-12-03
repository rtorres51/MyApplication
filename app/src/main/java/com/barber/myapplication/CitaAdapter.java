package com.barber.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.CitaViewHolder> {

    private List<Cita> citas;
    private OnItemLongClickListener longClickListener;

    // Constructor
    public CitaAdapter(List<Cita> citas) {
        this.citas = citas;
    }

    // Interfaz para click largo
    public interface OnItemLongClickListener {
        void onItemLongClick(Cita cita);
    }

    // Método para configurar el listener desde la actividad principal
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public CitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cita, parent, false);
        return new CitaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaViewHolder holder, int position) {
        // Obtiene la cita correspondiente
        Cita cita = citas.get(position);

        // Asigna los valores al ViewHolder
        holder.txtNombre.setText(cita.getNombre());
        holder.txtFecha.setText(cita.getFecha());
        holder.txtHora.setText(cita.getHora());

        // Configura el listener de click largo
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(cita); // Llama al método del listener
            }
            return true; // Indica que el evento fue consumido
        });
    }

    @Override
    public int getItemCount() {
        return citas.size();
    }

    // ViewHolder que representa cada item en el RecyclerView
    public static class CitaViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtFecha, txtHora;

        public CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txt_nombre);
            txtFecha = itemView.findViewById(R.id.txt_fecha);
            txtHora = itemView.findViewById(R.id.txt_hora);
        }
    }
}
