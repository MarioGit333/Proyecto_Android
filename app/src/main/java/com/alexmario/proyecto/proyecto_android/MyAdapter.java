package com.alexmario.proyecto.proyecto_android;

/**
 * Created by Alex on 11/01/2018.
 */

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private List<Ruta> values;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtHeader,txtFooter,txtFecha;
        public View layout;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            txtHeader =  v.findViewById(R.id.txtHeader);
            txtFooter =  v.findViewById(R.id.txtFooter);
            txtFecha = v.findViewById(R.id.txtFecha);
        }
    }

    public void add(int position, Ruta item) {
        values.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        values.remove(position);
        notifyItemRemoved(position);
    }

    public MyAdapter(List<Ruta> myDataset) {
        values = myDataset;
    }

    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.item_list, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final String distancia = values.get(position).getDistancia();
        final String tiempo = values.get(position).getTiempo();
        final String fecha = values.get(position).getFecha();

        holder.txtHeader.setText("Distancia: "+distancia+" km");
        holder.txtFecha.setText("Fecha: "+fecha.substring(0,16));
        holder.txtFooter.setText("Duración: "+tiempo);
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

}