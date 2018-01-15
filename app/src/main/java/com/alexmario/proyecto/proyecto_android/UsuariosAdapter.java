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

public class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.ViewHolder> {
    private List<Usuario> values;


    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView txtHeader, txtFooter;
        public View layout;

        public ViewHolder(View v) {
            super(v);
            layout = v;
            txtHeader = v.findViewById(R.id.txtNombre);
            txtFooter = v.findViewById(R.id.txtNumero);
        }
    }

    public void add(int position, Usuario item) {
        values.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        values.remove(position);
        notifyItemRemoved(position);
    }

    public UsuariosAdapter(List<Usuario> myDataset) {
        values = myDataset;
    }

    @Override
    public UsuariosAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.item_list_user, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final String nombre = values.get(position).getNombre();
        final String numero = values.get(position).getNumero();
        holder.txtHeader.setText(nombre);
        holder.txtFooter.setText(numero);
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

}