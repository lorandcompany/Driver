package lor.and.company.driver.adapters.filters;

import android.widget.Filter;

public abstract class CollectionsFilter extends Filter {

    protected abstract FilterResults performFiltering(CharSequence constraint, String order, String orderBy);

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {

    }
}
